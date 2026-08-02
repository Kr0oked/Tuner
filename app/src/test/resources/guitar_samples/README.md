# Guitar sample fixtures

Real recorded guitar audio, used by `GuitarSampleReplayTest` to replay the
production pitch-detection pipeline (`OnsetDetector` -> `PitchDetector` ->
`FrequencySmoothing` -> `NoteConfirmationGate`) against real signals instead
of synthetic sine waves, and to catch regressions in meter stability.

## Format

- Mono
- 44.1 kHz sample rate (matches `AudioRecord`'s capture format)
- 16-bit PCM or 32-bit IEEE float WAV

## Recording workflow

Record the whole guitar in one continuous take per tuning level, not one
file per string:

1. Tune all 6 strings against a reference tuner (e.g. Harley Benton GT6) to
   roughly the same offset (0 cents = in tune, or all detuned flat/sharp by
   about the same amount).
2. Record one take plucking all strings in order low-to-high (E2, A2, D3,
   G3, B3, E4), leaving a few seconds of silence between plucks and letting
   each one ring out naturally. Any format works (e.g. M4A from a phone
   voice recorder) - it gets converted while splitting. Don't trim the
   attack transient off any pluck, since onset handling needs it too.
3. Repeat for a flat-detuned take and a sharp-detuned take.

### Splitting a take into per-string fixtures

Find the silence gaps between plucks (read-only, writes nothing):
```
ffmpeg -i take.m4a -af silencedetect=noise=-30dB:d=0.3 -f null - 2>&1 | grep silence_
```
Then cut and convert each segment in one pass:
```
ffmpeg -i take.m4a -ss <start> -to <end> -ac 1 -ar 44100 -c:a pcm_s16le \
  guitar_samples/<Note><Octave>_<cents>cents.wav
```

## Filename convention

`<note><octave>_<signed-cents>cents.wav`, e.g. `E2_0cents.wav`,
`E2_-24cents.wav`, `A2_+25cents.wav`. `GuitarSampleReplayTest` parses the
target note and cents directly out of the filename, so accuracy here
matters - and since a reference tuner can't be dialed to an exact cents
value by ear, use the *actually measured* offset, not a nominal target.

If a filename's guess turns out wrong by more than the test's tolerance,
the test fails and the failure message includes the real `settled=[...]`
cents readings - rename the file to match and rerun.
