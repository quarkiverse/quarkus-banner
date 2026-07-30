# Quarkus Banner

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.quarkus-banner/quarkus-banner?logo=apache-maven&style=flat-square)](https://central.sonatype.com/artifact/io.quarkiverse.quarkus-banner/quarkus-banner-parent)
[![Build](https://github.com/quarkiverse/quarkus-banner/actions/workflows/build.yml/badge.svg)](https://github.com/quarkiverse/quarkus-banner/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0)

Generate a [FIGlet](https://en.wikipedia.org/wiki/FIGlet) ASCII-art startup banner for your Quarkus application — rendered at **build time** from a
piece of text and a font, and shown at start-up the same way Quarkus renders its own banner.

```text
  __  __         ____                  _
 |  \/  |_   _  / ___|  ___ _ ____   _(_) ___ ___
 | |\/| | | | | \___ \ / _ \ '__\ \ / / |/ __/ _ \
 | |  | | |_| |  ___) |  __/ |   \ V /| | (_|  __/
 |_|  |_|\__, | |____/ \___|_|    \_/ |_|\___\___|
         |___/

                         Powered by Quarkus 3.x.x
```

## How it works

At build time the extension renders your text into ASCII art with one of the ~250 bundled FIGlet fonts and installs it as a `TextBannerFormatter` on
the console log handler — the exact mechanism Quarkus uses for its own banner. This means:

- the banner appears as a header above the log output and honours your console format and colour settings;
- Quarkus' built-in banner is replaced automatically (you don't need to set `quarkus.banner.enabled=false`);
- if generation is disabled, or the text can't be rendered, the default Quarkus banner is left untouched.

Because the banner is baked at build time, changing the text or font requires a rebuild (or a live-reload in dev mode).

## Installation

Add the dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>io.quarkiverse.quarkus-banner</groupId>
    <artifactId>quarkus-banner</artifactId>
    <version>{version}</version>
</dependency>
```

For Gradle, add to your `build.gradle`:

```gradle
implementation("io.quarkiverse.quarkus-banner:quarkus-banner:{version}")
```

## Usage

Configure the banner in `application.properties`:

```properties
# The text to render (defaults to quarkus.application.name, or "Quarkus")
quarkus.banner-generator.text=My Service

# One of the bundled fonts (defaults to "standard")
quarkus.banner-generator.font=doom
```

## Configuration

All properties are fixed at build time.

| Property                            | Type      | Default                    | Description                                                                                                       |
|-------------------------------------|-----------|----------------------------|-------------------------------------------------------------------------------------------------------------------|
| `quarkus.banner-generator.enabled`  | `boolean` | `true`                     | Whether the banner is generated at build time. When `false`, Quarkus' own banner applies as usual.                |
| `quarkus.banner-generator.text`     | `string`  | `quarkus.application.name` | The text to render as a FIGlet banner.                                                                            |
| `quarkus.banner-generator.font`     | `enum`    | `standard`                 | The bundled font to use (see [Fonts](#fonts)). Matched case-insensitively; an unknown font is a build-time error. |
| `quarkus.banner-generator.power-by` | `boolean` | `true`                     | Append a right-aligned `Powered by Quarkus <version>` tagline under the banner.                                   |

## Fonts

`quarkus.banner-generator.font` must be one of the **246 FIGlet fonts** bundled with, and tested against, this extension — for example `standard`,
`slant`, `doom`, `big`, `colossal`, `banner3-D` or `3d_diagonal`. Only bundled fonts are accepted; arbitrary classpath resources, file paths and
remote URLs are intentionally not supported, and typos are caught at build time.

The full list of fonts and their authors is in [FIGLET-FONTS.md](FIGLET-FONTS.md).

> **Licensing note:** the bundled fonts originate from the FIGlet font collection and are authored by many
> individuals under varied terms. Each font's original header (including author credit) is preserved in the
> `.flf` file; they are redistributed on that basis and are **not** relicensed under this extension's
> Apache-2.0 license. Review individual font headers before redistributing.

## Dev mode

Quarkus' interactive dev console (`quarkus:dev`) repaints its pinned prompt and can erase the start-up banner on narrow terminals — this affects
Quarkus' own banner too, not just this extension. If you want the banner to survive in dev mode regardless of terminal width, use the basic console
**for the dev profile only**:

```properties
%dev.quarkus.console.basic=true
```

This has no effect in production (the interactive console only runs in dev and test modes).

## Documentation

Full documentation lives in the `docs/` directory and is published to
<https://docs.quarkiverse.io/quarkus-banner/dev/>.

## License

Apache License 2.0 — see [LICENSE](LICENSE). Bundled FIGlet fonts retain their original terms; see
[FIGLET-FONTS.md](FIGLET-FONTS.md).
