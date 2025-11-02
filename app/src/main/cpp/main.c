/*******************************************************************************************
*
*   raylib [models] example - yaw pitch roll
*
*   Example complexity rating: [★★☆☆] 2/4
*
*   Example originally created with raylib 1.8, last time updated with raylib 4.0
*
*   Example contributed by Berni (@Berni8k) and reviewed by Ramon Santamaria (@raysan5)
*
*   Example licensed under an unmodified zlib/libpng license, which is an OSI-certified,
*   BSD-like license that allows static linking with closed source software
*
*   Copyright (c) 2017-2025 Berni (@Berni8k) and Ramon Santamaria (@raysan5)
*
********************************************************************************************/

#include "raymob.h"

#include "raymath.h"        // Required for: MatrixRotateXYZ()

#define RAYGUI_IMPLEMENTATION
#include "raygui.h"

// ------------------------------------------------------------------------------------
// Private
// ------------------------------------------------------------------------------------

static volatile float pitch = 0.0f;
static volatile float roll = 0.0f;
static volatile float yaw = 0.0f;
static float acceleration = 0.02f;
static float rotationSpeed = 1.0f;

void updateOrientationValues(float x, float y, float z) {
    pitch = x;
    roll = y;
    yaw = z;
}

// ------------------------------------------------------------------------------------
// JNI functions
// ------------------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_example_esp32_1mpu6050_1mobile_1data_1collection_raylib_NativeBridge_updateOrientation(
        JNIEnv *env, jobject thiz, jfloat x, jfloat y, jfloat z) {
    updateOrientationValues(x, y, z);
}

//------------------------------------------------------------------------------------
// Program main entry point
//------------------------------------------------------------------------------------
int main(void)
{
    // Initialization
    //--------------------------------------------------------------------------------------
    const int screenWidth = 480;
    const int screenHeight = 1066;

    //SetConfigFlags(FLAG_MSAA_4X_HINT | FLAG_WINDOW_HIGHDPI);
    InitWindow(screenWidth, screenHeight, "raylib [models] example - yaw pitch roll");

    GuiSetFont(GetFontDefault());
    GuiLoadStyleDefault(); // reset style after drawing

    Camera camera = { 0 };
    camera.position = (Vector3){ 0.0f, 15.0f, -34.0f };// Camera position perspective
    camera.target = (Vector3){ 0.0f, 0.0f, 0.0f };      // Camera looking at point
    camera.up = (Vector3){ 0.0f, 1.0f, 0.0f };          // Camera up vector (rotation towards target)
    camera.fovy = 30.0f;                                // Camera field-of-view Y
    camera.projection = CAMERA_PERSPECTIVE;             // Camera type

    Model model = LoadModel("modelResources/models/gltf/esp8266.glb");                  // Load model
//    Texture2D texture = LoadTexture("modelResources/models/obj/plane_diffuse.png");  // Load model texture
//    model.materials[0].maps[MATERIAL_MAP_DIFFUSE].texture = texture;            // Set map diffuse texture

    char textMessage [128];

    SetTargetFPS(60);               // Set our game to run at 60 frames-per-second
    //--------------------------------------------------------------------------------------

    // Main game loop
    while (!WindowShouldClose())    // Detect window close button or ESC key
    {
        // Update
        //----------------------------------------------------------------------------------

        // ------------------ Create Transformation ------------------
        //rotationSpeed += acceleration;
//        pitch = pitch + rotationSpeed;
//        yaw = yaw + rotationSpeed;
//        roll = roll + rotationSpeed;
        // -----------------------------------------------------------

        // Transformation matrix for rotations
        model.transform = MatrixRotateXYZ((Vector3){ DEG2RAD*pitch, DEG2RAD*yaw, DEG2RAD*roll });
        //----------------------------------------------------------------------------------

        // Draw
        //----------------------------------------------------------------------------------
        BeginDrawing();

        ClearBackground(RAYWHITE);

        // Draw 3D model (recommended to draw 3D always before 2D)
        BeginMode3D(camera);

        DrawModel(model, (Vector3){ 0.0f, 0.0f, 0.0f }, 1.0f, WHITE);   // Draw 3d model with texture
        DrawGrid(10, 1.5f);

        EndMode3D();

//        // Draw controls info
//        DrawRectangle(30, 370, 260, 70, Fade(GREEN, 0.5f));
//        DrawRectangleLines(30, 370, 260, 70, Fade(DARKGREEN, 0.5f));
//        DrawText("Pitch controlled with: KEY_UP / KEY_DOWN", 40, 380, 10, DARKGRAY);
//        DrawText("Roll controlled with: KEY_LEFT / KEY_RIGHT", 40, 400, 10, DARKGRAY);
//        DrawText("Yaw controlled with: KEY_A / KEY_S", 40, 420, 10, DARKGRAY);
//
//        DrawText("(c) WWI Plane Model created by GiaHanLam", screenWidth - 240, screenHeight - 20, 10, DARKGRAY);
        sprintf(textMessage, "Value: %f, %f, %f", pitch, roll, yaw);
        DrawText(textMessage, 20, 20, 30, DARKBLUE);

        if (GuiButton((Rectangle){4.0f, 4.0f, 100.0f, 40.0f}, "Back")) {
            EndDrawing();
            break;
        }

        EndDrawing();
        //----------------------------------------------------------------------------------
    }

    // De-Initialization
    //--------------------------------------------------------------------------------------
    UnloadModel(model);     // Unload model data
    //UnloadTexture(texture); // Unload texture data

    CloseWindow();          // Close window and OpenGL context
    //--------------------------------------------------------------------------------------

    return 0;
}
