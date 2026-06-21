#ifndef _COMMON_H
#define _COMMON_H

#ifdef __cplusplus
extern "C" {            // Prevents name mangling of functions
#endif

/*
 * Virtual Resolution: the fixed coordinate system used to design and
 * render the entire app consistently across all devices.
 *
 * NOTE:
 * - `RenderTexture` is used as a fixed-size offscreen canvas (buffer)
 * - All rendering happens into this texture using virtual coordinates
 * - `DrawTexturePro` is used at the end to scale and present that buffer
 *   onto the actual screen resolution
 *
 */

// Ratio: 2.16
#define VIRTUAL_WIDTH  390
#define VIRTUAL_HEIGHT 844

#ifdef __cplusplus
}
#endif

#endif
