/***********************************************************************************
 * Derived from a raylib example.
 **********************************************************************************/

#include "raylib.h"
#include "screens.h"
#include <math.h>
#include "common.h"

//----------------------------------------------------------------------------------
// Module Variables Definition (local)
//----------------------------------------------------------------------------------
static int framesCounter = 0;
static int finishScreen = 0;

static int logoPositionX = 0;
static int logoPositionY = 0;

static int lettersCount = 0;

static int topSideRecWidth = 0;
static int leftSideRecHeight = 0;

static int bottomSideRecWidth = 0;
static int rightSideRecHeight = 0;

static int state = 0;              // Logo animation states
static float alpha = 1.0f;         // Useful for fading

//----- Shader & Texture ---------------------------------------

static Shader glitchShader = { 0 };

static Shader crtShader = { 0 };
float controlCrtWarp = 1.5f;

static int timeLoc = -1;
static int warpLoc = -1;

static float time = 0.0f;

static RenderTexture2D logoTarget;

//--------------------------------------------------------------

#if DEBUGMODE
void ReloadShader(void)
{
    UnloadShader(crtShader);

    crtShader = LoadShader(
        0,
        "resources/shaders/crtShader.fs"
    );

    timeLoc = GetShaderLocation(crtShader, "time");
}
#endif

//----------------------------------------------------------------------------------
// Logo Screen Functions Definition
//----------------------------------------------------------------------------------

// Logo Screen Initialization logic
void InitLogoScreen(void)
{
    finishScreen = 0;
    framesCounter = 0;
    lettersCount = 0;

    topSideRecWidth = 16;
    leftSideRecHeight = 16;
    bottomSideRecWidth = 16;
    rightSideRecHeight = 16;

    state = 0;
    alpha = 1.0f;

    // Shader & Texture
    
    crtShader = LoadShader( 0, "resources/shaders/crtShader.fs");

    timeLoc = GetShaderLocation(crtShader, "time");

    logoTarget = LoadRenderTexture(
        VIRTUAL_WIDTH,
        VIRTUAL_HEIGHT
    );

    logoPositionX = VIRTUAL_WIDTH / 2 - 128;
    logoPositionY = VIRTUAL_HEIGHT / 2 - 128;
    TraceLog(LOG_INFO, "logoPos = %d, %d", logoPositionX, logoPositionY);

    // Disable texture filtering ; for pixely affect
    SetTextureFilter(logoTarget.texture, TEXTURE_FILTER_POINT);

    int resolutionLoc = GetShaderLocation(crtShader, "resolution");
    Vector2 resolution = {
        (float)VIRTUAL_WIDTH,
        (float)VIRTUAL_HEIGHT
    };
    SetShaderValue(
        crtShader,
        resolutionLoc,
        &resolution,
        SHADER_UNIFORM_VEC2
    );

    warpLoc = GetShaderLocation(crtShader, "control_warp");
    SetShaderValue(
        crtShader,
        warpLoc,
        &controlCrtWarp,
        SHADER_UNIFORM_FLOAT
    );

}

// Logo Screen Update logic
void UpdateLogoScreen(void)
{
    time += GetFrameTime();

    //----- Update Shaders------------------------------------------

    SetShaderValue(
        crtShader,
        timeLoc,
        &time,
        SHADER_UNIFORM_FLOAT
    );

    SetShaderValue(
            crtShader,
            warpLoc,
            &controlCrtWarp,
            SHADER_UNIFORM_FLOAT
            );

    //--------------------------------------------------------------

#if DEBUGMODE
    if (IsKeyPressed(KEY_R))
    {
        ReloadShader();
    }
#endif

    if (state == 0)                 // State 0: Top-left square corner blink logic
    {
        framesCounter++;

        controlCrtWarp += 0.02;

        if (framesCounter == 40)
        {
            state = 1;
            framesCounter = 0;      // Reset counter... will be used later...
        }
    }
    else if (state == 1)            // State 1: Bars animation logic: top and left
    {
        topSideRecWidth += 8;
        leftSideRecHeight += 8;

        if (topSideRecWidth == 256) state = 2;
    }
    else if (state == 2)            // State 2: Bars animation logic: bottom and right
    {
        bottomSideRecWidth += 8;
        rightSideRecHeight += 8;

        if (bottomSideRecWidth == 256) state = 3;
    }
    else if (state == 3)            // State 3: "repspectre" text-write animation logic
    {
        framesCounter++;

        if (lettersCount < 10)
        {
            if (framesCounter/5)   // Every 5 frames, one more letter!
            {
                lettersCount++;
                framesCounter = 0;
            }
        }
#if !DEBUGMODE
        else    // When all letters have appeared, just fade out everything
        {
            if (framesCounter > 200)
            {
                alpha -= 0.02f;

                if (alpha <= 0.0f)
                {
                    alpha = 0.0f;
                    finishScreen = 1;   // Jump to next screen
                }
            }
        }
#endif
    }
}

// Logo Screen Draw logic
void DrawLogoScreen(void)
{
    BeginTextureMode(logoTarget);

    ClearBackground(WHITE);

    if (state == 0)         // Draw blinking top-left square corner
    {
        if ((framesCounter/10)%2) DrawRectangle(logoPositionX, logoPositionY, 16, 16, BLACK);
    }
    else if (state == 1)    // Draw bars animation: top and left
    {
        DrawRectangle(logoPositionX, logoPositionY, topSideRecWidth, 16, BLACK);
        DrawRectangle(logoPositionX, logoPositionY, 16, leftSideRecHeight, BLACK);
    }
    else if (state == 2)    // Draw bars animation: bottom and right
    {
        DrawRectangle(logoPositionX, logoPositionY, topSideRecWidth, 16, BLACK);
        DrawRectangle(logoPositionX, logoPositionY, 16, leftSideRecHeight, BLACK);

        DrawRectangle(logoPositionX + 240, logoPositionY, 16, rightSideRecHeight, BLACK);
        DrawRectangle(logoPositionX, logoPositionY + 240, bottomSideRecWidth, 16, BLACK);
    }
    else if (state == 3)    // Draw "repspectre" text-write animation + "powered by"
    {
        DrawRectangle(logoPositionX, logoPositionY, topSideRecWidth, 16, Fade(BLACK, alpha));
        DrawRectangle(logoPositionX, logoPositionY + 16, 16, leftSideRecHeight - 32, Fade(BLACK, alpha));

        DrawRectangle(logoPositionX + 240, logoPositionY + 16, 16, rightSideRecHeight - 32, Fade(BLACK, alpha));
        DrawRectangle(logoPositionX, logoPositionY + 240, bottomSideRecWidth, 16, Fade(BLACK, alpha));

        DrawRectangle(logoPositionX + 16, logoPositionY + 16, 224, 224, Fade(RAYWHITE, alpha));

        DrawText(TextSubtext("repspectre", 0, lettersCount), logoPositionX + 40, logoPositionY + leftSideRecHeight/2, 30, Fade(BLACK, alpha));

        if (framesCounter > 20) DrawText("powered by", logoPositionX, logoPositionY - 27, 20, Fade(DARKGRAY, alpha));
    }

    EndTextureMode();

    //--------------------------------------------------------------

    BeginShaderMode(crtShader);

    // upscale to actual screen
    DrawTexturePro(
        logoTarget.texture,
        (Rectangle){ 0, 0,
        (float)logoTarget.texture.width,
        -(float)logoTarget.texture.height },
        (Rectangle){ 0, 0,
        (float)GetScreenWidth(),
        (float)GetScreenHeight() },
        (Vector2){0, 0},
        0,
        WHITE
    );

    EndShaderMode();
}

// Logo Screen Unload logic
void UnloadLogoScreen(void)
{
    // Unload LOGO screen variables here!
    UnloadShader(crtShader);
}

// Logo Screen should finish?
int FinishLogoScreen(void)
{
    return finishScreen;
}
