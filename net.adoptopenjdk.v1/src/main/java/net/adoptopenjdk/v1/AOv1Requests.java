/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adoptopenjdk.v1;

import net.adoptopenjdk.spi.AOAPIRequestsType;
import net.adoptopenjdk.spi.AOAPIVersionProviderType;
import net.adoptopenjdk.spi.AOException;
import net.adoptopenjdk.spi.AORelease;
import net.adoptopenjdk.spi.AOVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The default implementation of the API 1.0 request provider.
 */

public final class AOv1Requests implements AOv1RequestsType
{
  private static final Logger LOG = LoggerFactory.getLogger(AOv1Requests.class);

  private final AOv1HTTPConnectionsType connections;
  private final ResourceBundle messages;
  private final AOv1Parser parser;
  private RateLimit rate_limit;

  private AOv1Requests(
    final AOv1Parser in_parser,
    final AOv1HTTPConnectionsType in_connections,
    final RateLimit in_rate_limit)
  {
    this.parser =
      Objects.requireNonNull(in_parser, "parser");
    this.connections =
      Objects.requireNonNull(in_connections, "connections");
    this.rate_limit =
      Objects.requireNonNull(in_rate_limit, "rate limit");

    this.messages =
      ResourceBundle.getBundle("net/adoptopenjdk/v1/messages");
  }

  /**
   * Create a new 1.0 request provider.
   *
   * @return A new provider
   *
   * @throws AOException On initialization errors
   */

  public static AOv1RequestsType open()
    throws AOException
  {
    return open(new AOv1HTTPConnections());
  }

  /**
   * Create a new 1.0 request provider using the given HTTP connection provider.
   *
   * @param connections The HTTP connection provider
   *
   * @return A new request provider
   *
   * @throws AOException On initialization errors
   */

  public static AOv1RequestsType open(
    final AOv1HTTPConnectionsType connections)
    throws AOException
  {
    Objects.requireNonNull(connections, "connections");
    return new AOv1Requests(
      new AOv1Parser(),
      connections,
      checkRateLimit(connections));
  }

  private static RateLimit checkRateLimit(
    final AOv1HTTPConnectionsType connections)
    throws AOException
  {
    Objects.requireNonNull(connections, "connections");

    final Map<String, String> props = standardProperties();
    try (AOv1HTTPConnectionType connection =
           connections.head(
             URI.create("https://api.adoptopenjdk.net/"),
             props)) {
      return rateLimitForConnection(connection);
    } catch (final IOException e) {
      throw new AOException(e.getMessage(), e);
    }
  }

  private static Map<String, String> standardProperties()
  {
    return Map.of("User-Agent", userAgent());
  }

  private static String userAgent()
  {
    final String v = AOv1Requests.class.getPackage().getImplementationVersion();
    return "net.adoptopenjdk.api " + ((v == null) ? "0.0.0" : v);
  }

  private static RateLimit rateLimitForConnection(
    final AOv1HTTPConnectionType connection)
  {
    final Map<String, List<String>> headers = connection.headers();

    final int retry;
    final int remaining;
    if (headers.containsKey("X-RateLimit-Remaining")) {
      remaining = Integer.valueOf(headers.get("X-RateLimit-Remaining").get(0)).intValue();
      if (remaining == 0) {
        if (headers.containsKey("Retry-After")) {
          retry = Integer.valueOf(headers.get("Retry-After").get(0)).intValue();
        } else {
          LOG.warn("unable to determine retry time");
          retry = 0;
        }
      } else {
        retry = 3600;
      }
    } else {
      LOG.warn("unable to determine remaining request limit");
      remaining = Integer.MAX_VALUE;
      retry = 3600;
    }

    return new RateLimit(remaining, retry);
  }

  @Override
  public int rateLimitRemaining()
  {
    return this.rate_limit.remaining;
  }

  @Override
  public List<AOVariant> variants()
    throws AOException
  {
    this.checkRateLimitRemaining();

    final URI target = URI.create("https://api.adoptopenjdk.net/variants");
    try (AOv1HTTPConnectionType connection =
           this.connections.get(target, standardProperties())) {

      this.rate_limit = rateLimitForConnection(connection);
      try (InputStream stream = connection.input()) {
        return this.parser.parseVariants(target, stream);
      }
    } catch (final IOException e) {
      throw new AOException(e.getMessage(), e);
    }
  }

  @Override
  public List<AORelease> releasesForVariant(final String variant)
    throws AOException
  {
    Objects.requireNonNull(variant, "variant");

    this.checkRateLimitRemaining();

    final URI target =
      URI.create("https://api.adoptopenjdk.net/" + variant + "/releases");

    try (AOv1HTTPConnectionType connection =
           this.connections.get(target, standardProperties())) {

      this.rate_limit = rateLimitForConnection(connection);
      try (InputStream stream = connection.input()) {
        return this.parser.parseReleases(target, stream);
      }
    } catch (final IOException e) {
      throw new AOException(e.getMessage(), e);
    }
  }

  @Override
  public List<AORelease> nightlyBuildsForVariant(final String variant)
    throws AOException
  {
    Objects.requireNonNull(variant, "variant");

    this.checkRateLimitRemaining();

    final URI target =
      URI.create("https://api.adoptopenjdk.net/" + variant + "/nightly");

    try (AOv1HTTPConnectionType connection =
           this.connections.get(target, standardProperties())) {

      this.rate_limit = rateLimitForConnection(connection);
      try (InputStream stream = connection.input()) {
        return this.parser.parseReleases(target, stream);
      }
    } catch (final IOException e) {
      throw new AOException(e.getMessage(), e);
    }
  }

  private void checkRateLimitRemaining()
    throws AOException
  {
    if (this.rate_limit.remaining <= 0) {
      throw new AOException(
        MessageFormat.format(
          this.messages.getString("api_rate_limit_exceeded"),
          Integer.valueOf(this.rate_limit.retry_in)));
    }
  }

  /**
   * A provider for the {@link AOAPIVersionProviderType} interface.
   */

  public static final class Provider implements AOAPIVersionProviderType
  {
    /**
     * Construct a provider. This method is intended to be called
     * by {@link java.util.ServiceLoader}.
     */

    public Provider()
    {

    }

    /**
     * Create a new 1.0 request provider. This method is intended to be called
     * by {@link java.util.ServiceLoader}.
     *
     * @return A new provider
     */

    public static AOAPIVersionProviderType provider()
    {
      return new Provider();
    }

    @Override
    public int supportedMajorAPI()
    {
      return 1;
    }

    @Override
    public AOAPIRequestsType create()
      throws AOException
    {
      return open();
    }
  }

  private static final class RateLimit
  {
    private final int remaining;
    private final int retry_in;

    RateLimit(
      final int in_remaining,
      final int in_retry_in)
    {
      this.remaining = in_remaining;
      this.retry_in = in_retry_in;
    }
  }
}