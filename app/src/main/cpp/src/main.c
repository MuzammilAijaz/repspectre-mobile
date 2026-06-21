/***********************************************************************************
 * Derived from a raylib example.
 **********************************************************************************/

#ifdef PLATFORM_ANDROID
#include "raymob.h"
#else
#include "raylib.h"
#endif

#include "rlgl.h"
#include "raymath.h"        // Required for: MatrixRotateXYZ()
#include "screens.h"
#include "common.h"

#define RAYGUI_IMPLEMENTATION
#include "raygui.h"

Font font = { 0 };

//----- Screen ------------------------------------------------------------------------
GameScreen currentScreen = LOGO;
// Required variables to manage screen transitions (fade-in, fade-out)
static float transAlpha = 0.0f;
static bool onTransition = false;
static bool transFadeOut = false;
static int transFromScreen = -1;
static GameScreen transToScreen = UNKNOWN;

static void ChangeToScreen(int screen);     // Change to screen, no transition effect

static void UpdateDrawFrame(void);          // Update and draw one frame

//------------------------------------------------------------------------------------
// Program main entry point
//------------------------------------------------------------------------------------
int main(void)
{
    // Initialization
    //--------------------------------------------------------------------------------------
    const int screenWidth = VIRTUAL_WIDTH;
    const int screenHeight = VIRTUAL_HEIGHT;

    // ========================================================
    // DEBUG:
    // -------------------- iPhone / Apple --------------------
    // const int screenWidth  = 375;  const int screenHeight = 667;   // iPhone SE (1st/2nd gen) ~16:9 (1.78)
    // const int screenWidth  = 390;  const int screenHeight = 844;   // iPhone 12 / 13 / 14 ~19.5:9 (2.16)
    // const int screenWidth  = 428;  const int screenHeight = 926;   // iPhone 12/13/14 Pro Max ~19.5:9 (2.16)
    // const int screenWidth  = 430;  const int screenHeight = 932;   // iPhone 14/15 Pro Max ~19.5:9 (2.17)
    // -------------------- Google Pixel --------------------
    // const int screenWidth  = 393;  const int screenHeight = 851;   // Pixel 5 / Pixel 6 class ~19:9 (2.17)
    // -------------------- Samsung Galaxy --------------------
    // const int screenWidth  = 360;  const int screenHeight = 780;   // Budget Samsung Android ~19.5:9
    // const int screenWidth  = 412;  const int screenHeight = 915;   // Galaxy S21 / S22 / S23 ~19.8:9 (2.22)
    // const int screenWidth  = 480;  const int screenHeight = 960;   // Older tall Android ~18:9 (2.0)
    // -------------------- Experimental / Stress Tests --------------------
    // const int screenWidth  = 480;  const int screenHeight = 800;   // 16:10 (1.6) compressed height test
    // const int screenWidth  = 480;  const int screenHeight = 1000;  // mid tall stress test (~2.08)
    // const int screenWidth  = 480;  const int screenHeight = 1200;  // ultra tall UI stress test (~2.5)
    // -------------------- Wide / fallback desktop-ish --------------------
    // const int screenWidth  = 540;  const int screenHeight = 960;   // wide Android tablet-ish feel
    // const int screenWidth  = 720;  const int screenHeight = 1280;  // HD baseline (1.78)
    // ========================================================

    //SetConfigFlags(FLAG_MSAA_4X_HINT | FLAG_WINDOW_HIGHDPI);
    InitWindow(screenWidth, screenHeight, "raylib [models] example - yaw pitch roll");

    font = GetFontDefault();
    GuiLoadStyleDefault(); // reset style after drawing

    // Setup and init first screen
    currentScreen = LOGO;
    InitLogoScreen();

    SetTargetFPS(60);               // Set our game to run at 60 frames-per-second
    //--------------------------------------------------------------------------------------

    // Main game loop
    while (!WindowShouldClose())    // Detect window close button or ESC key
    {
        // Update
        //----------------------------------------------------------------------------------

        UpdateDrawFrame();

        //----------------------------------------------------------------------------------
    }

    // De-Initialization
    //--------------------------------------------------------------------------------------

    switch (currentScreen)
    {
        case LOGO: UnloadLogoScreen(); break;
        default: break;
    }
    //UnloadTexture(texture); // Unload texture data

    CloseWindow();          // Close window and OpenGL context
    //--------------------------------------------------------------------------------------

    return 0;
}

// ------------------------------------------------------------------------------------
// My Functions
// ------------------------------------------------------------------------------------


//----------------------------------------------------------------------------------
// Module Functions Definition
//----------------------------------------------------------------------------------
// Change to next screen, no transition
static void ChangeToScreen(int screen)
{
    // Unload current screen
    switch (currentScreen)
    {
        case LOGO: UnloadLogoScreen(); break;
        case VISUALIZER: UnloadVisualizerScreen(); break;
        default: break;
    }

    // Init next screen
    switch (screen)
    {
        case LOGO: InitLogoScreen(); break;
        case VISUALIZER: InitVisualizerScreen(); break;
        default: break;
    }

    currentScreen = screen;
}

// Request transition to next screen
static void TransitionToScreen(int screen)
{
    onTransition = true;
    transFadeOut = false;
    transFromScreen = currentScreen;
    transToScreen = screen;
    transAlpha = 0.0f;
}

// Update transition effect (fade-in, fade-out)
static void UpdateTransition(void)
{
    if (!transFadeOut)
    {
        transAlpha += 0.05f;

        // NOTE: Due to float internal representation, condition jumps on 1.0f instead of 1.05f
        // For that reason we compare against 1.01f, to avoid last frame loading stop
        if (transAlpha > 1.01f)
        {
            transAlpha = 1.0f;

            // Unload current screen
            switch (transFromScreen)
            {
                case LOGO: UnloadLogoScreen(); break;
                case VISUALIZER: UnloadVisualizerScreen(); break;
                default: break;
            }

            // Load next screen
            switch (transToScreen)
            {
                case LOGO: InitLogoScreen(); break;
                case VISUALIZER: InitVisualizerScreen(); break;
                default: break;
            }

            currentScreen = transToScreen;

            // Activate fade out effect to next loaded screen
            transFadeOut = true;
        }
    }
    else  // Transition fade out logic
    {
        transAlpha -= 0.02f;

        if (transAlpha < -0.01f)
        {
            transAlpha = 0.0f;
            transFadeOut = false;
            onTransition = false;
            transFromScreen = -1;
            transToScreen = UNKNOWN;
        }
    }
}

// Draw transition effect (full-screen rectangle)
static void DrawTransition(void)
{
    DrawRectangle(0, 0, GetScreenWidth(), GetScreenHeight(), Fade(BLACK, transAlpha));
}

// Update and draw game frame
static void UpdateDrawFrame(void)
{
    // Update
    //----------------------------------------------------------------------------------
    //UpdateMusicStream(music);       // NOTE: Music keeps playing between screens

    if (!onTransition)
    {
        switch(currentScreen)
        {
            case LOGO:
            {
                UpdateLogoScreen();

                if (FinishLogoScreen()) TransitionToScreen(VISUALIZER);

            } break;
            case VISUALIZER:
            {
                UpdateVisualizerScreen();

                if (FinishVisualizerScreen() == 1) TransitionToScreen(LOGO);

            } break;
            default: break;
        }
    }
    else UpdateTransition();    // Update transition (fade-in, fade-out)
    //----------------------------------------------------------------------------------

    // Draw
    //----------------------------------------------------------------------------------
    BeginDrawing();

        ClearBackground(RAYWHITE);

        switch(currentScreen)
        {
            case LOGO: DrawLogoScreen(); break;
            case VISUALIZER: DrawVisualizerScreen(); break;
            default: break;
        }

        // Draw full screen rectangle in front of everything
        if (onTransition) DrawTransition();

        //DrawFPS(10, 10);

    EndDrawing();
    //----------------------------------------------------------------------------------
}
