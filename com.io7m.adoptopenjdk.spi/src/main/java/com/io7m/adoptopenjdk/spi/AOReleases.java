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

package com.io7m.adoptopenjdk.spi;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Functions to manipulate releases.
 */

public final class AOReleases
{
  private AOReleases()
  {

  }

  /**
   * Filter the binaries in the given release, returning a release that contains
   * only the matching binaries.
   *
   * @param release      The initial release
   * @param os           The operating system, if any
   * @param architecture The architecture, if any
   *
   * @return A filtered release
   *
   * @see AOBinaryType#hasOSAndArch(Optional, Optional)
   */

  public static AORelease releaseWithMatchingBinaries(
    final AORelease release,
    final Optional<String> os,
    final Optional<String> architecture)
  {
    Objects.requireNonNull(release, "release");
    Objects.requireNonNull(os, "os");
    Objects.requireNonNull(architecture, "architecture");

    return AORelease.builder()
      .from(release)
      .setBinaries(
        release.binaries()
          .stream()
          .filter(binary -> binary.hasOSAndArch(os, architecture))
          .collect(Collectors.toList()))
      .build();
  }

}
