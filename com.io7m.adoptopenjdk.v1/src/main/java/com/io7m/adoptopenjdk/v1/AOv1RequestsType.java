/*
 * Copyright © 2018 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.adoptopenjdk.v1;

import com.io7m.adoptopenjdk.spi.AOException;
import com.io7m.adoptopenjdk.spi.AORelease;
import com.io7m.adoptopenjdk.spi.AOReleases;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The type of requests that can be made to the remote server.
 */

public interface AOv1RequestsType
{
  /**
   * @return The number of requests that can be made before the server begins rejecting requests (for rate limiting)
   */

  int rateLimitRemaining();

  /**
   * List the available releases on the server.
   *
   * @param variant The build variant
   *
   * @return A list of available releases
   *
   * @throws AOException On any and all errors
   */

  List<AORelease> releasesForVariant(
    String variant)
    throws AOException;

  /**
   * List the available releases on the server, filtering by the given
   * optional information.
   *
   * @param variant      The build variant
   * @param os           The operating system, if any
   * @param architecture The architecture, if any
   *
   * @return A list of available releases
   *
   * @throws AOException On any and all errors
   */

  default List<AORelease> releasesForVariantWith(
    final String variant,
    final Optional<String> os,
    final Optional<String> architecture)
    throws AOException
  {
    Objects.requireNonNull(variant, "variant");
    Objects.requireNonNull(os, "operating_system");
    Objects.requireNonNull(architecture, "architecture");

    return this.releasesForVariant(variant)
      .stream()
      .filter(r -> r.hasBinaryWith(os, architecture))
      .map(r -> AOReleases.releaseWithMatchingBinaries(r, os, architecture))
      .collect(Collectors.toList());
  }
}
