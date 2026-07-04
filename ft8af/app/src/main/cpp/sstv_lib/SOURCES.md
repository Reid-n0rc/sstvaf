# sstv_lib — specification sources

This directory is a **clean-room** SSTV codec implementation, written for the
MIT-licensed SSTVAF project. It was implemented directly from published mode
*specifications* (timing tables, tone frequencies, VIS code assignments) — not
from any existing SSTV codec source code. In particular, no GPL SSTV source
(QSSTV, slowrx, MMSSTV, or derivatives) was consulted or reproduced.

## Specification sources

1. **JL Barber, N7CXI, "Proposal for SSTV Mode Specifications", presented at
   the Dayton SSTV forum, May 20 2000.**
   The primary source. Provides, for each mode implemented here (Robot 36/72,
   Martin M1/M2, Scottie S1/S2, PD-50/90/120):
   - the per-line segment structure and all segment durations in µs,
   - sync (1200 Hz), black (1500 Hz), white (2300 Hz) reference frequencies,
   - the calibration header layout (300 ms 1900 Hz leader, 10 ms 1200 Hz
     break, 300 ms 1900 Hz leader, VIS code),
   - the VIS format (30 ms cells, 1200 Hz start/stop bits, 1100 Hz = bit 1,
     1300 Hz = bit 0, 7 data bits LSB first, even parity), and
   - the 7-bit VIS code assignments (Robot36=8, Robot72=12, Martin2=40,
     Martin1=44, Scottie2=56, Scottie1=60, PD50=93, PD120=95, PD90=99).

2. **ITU-R Recommendation BT.601** (studio-swing YCbCr conversion constants,
   as tabulated in standard references, e.g. C. Poynton, "Digital Video and
   HD"): the forward luma/chroma matrix used by the Robot and PD family:

   ```
   Y  =  16 + ( 65.738 R + 129.057 G +  25.064 B) / 256
   Cr = 128 + (112.439 R -  94.154 G -  18.285 B) / 256   (the "R-Y" scan)
   Cb = 128 + (-37.945 R -  74.494 G + 112.439 B) / 256   (the "B-Y" scan)
   ```

   The decoder uses the exact numeric inverse of this matrix (computed to
   double precision in `sstv_color.c`), with the result clamped to [0, 255].

3. **Pixel value ↔ frequency mapping** (from source 1): a pixel value
   v ∈ [0, 255] maps to f = 1500 + v · (800 / 255) Hz.

Everything else here (oscillator, FM demodulator, sync tracker, slant
regression) is original signal-processing code written for this project.
