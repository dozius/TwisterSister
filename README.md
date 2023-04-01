# Twister Sister Bitwig Controller Extension

[![Download](https://img.shields.io/github/downloads/dozius/TwisterSister/total.svg)](https://github.com/dozius/TwisterSister/releases/latest)
[![Donate](https://img.shields.io/badge/donate-paypal-blue.svg)](https://www.paypal.me/cisc)

This is the source code repository for the Twister Sister Bitwig controller extension.

Precompiled releases can be found [here](https://github.com/dozius/TwisterSister/releases).

User documentation, including installation instructions, can be found [here](docs/README.md).

# Fork considerations

This is twister sister, with those mods: 

- replace user controls and pan knob with more sends
- add bank2 , controlling 4 consecutive tracks (faders on 4th row), and 3 sends (3 first rows)
    - move banks button to the left side by default
    - toggle arm - click send2 knob
    - toggle solo - click send3 knob
    - toggle mute - click volume knob
    - pan - click + volume
- add a toggle to set a MFT as an "extender", when a track has this option enabled:
    - banks button are on the right side
    - track controls (banks) are offset by 4 tracks (you get an 8 fader page)
- Bank2/knob4 (top-right): click = next fader page
- Bank2/knob3 (top-right): click = previous fader page
- Bank2/knob3-4 (top-right): long-click = get in/out group

- Bank 3 can be a EQ+ control
- Added a 5th bank for user.

## Eq Mode

If you check `Enable EQ 3rd band`, the 3rd bank become the closest EQ+ controller.

- Bank3/knob on 1st and 3rd row:
    - twist = set frequency
    - click + twist = set Q
    - double-click = mute band (freq led turn off)
- bank3/knob on 2nd and 4th row:
    - twist = set gain
    - click + twist = set filter type
    - double-click = turn band off (re-enable by setting a filter type)
- RGB under knob matches Bitwig' :rainbow: frequency display

## dual twister mode

- enable dual mode for 8-track pages
- set a second twister in dual mode + extender to get 4-8 tracks' control
- in extender mode, bank buttons are on the right

When switching mode, you need to restart the extensions in order to process the config change

## two single mode twister

This will behave poorly as both twisters will bind to the first 4-tracks

## more twisters

hack it yourself.

## issues

behaves poorly when tracks are pinned

# build

## Compiling

### Requirements

- [OpenJDK 12.x](https://adoptopenjdk.net/releases.html?variant=openjdk12)
- [Maven >= 3.1.0](https://maven.apache.org/)

### Build and install

1. Follow the installation instructions for each of the above requirements.
2. Run `mvn install`.

### Debugging

1. Set an environment variable `BITWIG_DEBUG_PORT` to an unused port number.
2. Restart Bitwig.
3. Setup your debugger to connect to the port from step 1.
