/***********************************************************************************
 * Derived from a raylib example.
 **********************************************************************************/

#ifdef PLATFORM_ANDROID
#include "raymob.h"
#else
#include "raylib.h"
#endif

// #include "rlgl.h"
#include "raymath.h"        // Required for: MatrixRotateXYZ()
#include "screens.h"
#include "common.h"

#include "raygui.h"

#include <stdlib.h> // for exit
#include <stdio.h>

//----------------------------------------------------------------------------------
// Module Variables Definition (local)
//----------------------------------------------------------------------------------
static int framesCounter = 0;
static int finishScreen = 0;
static Model model;
static Camera camera = { 0 };
static Matrix baseTransform;
static char textMessage [128];

static volatile Quaternion quaternion = {
    0.f,
    0.f,
    0.f,
    0.f,
};
bool isQuaternionInitialized = false;

//----- Shader -------------------------------------------------

static Shader crtShader = { 0 };
static float controlCrtWarp = 3.0f;
static int timeLoc = -1;
static int warpLoc = -1;
static float time = 0.0f;

static RenderTexture2D visualizerTarget;

//--------------------------------------------------------------

void UpdateOrbitalCamera(Camera *camera, float deltaTime);

bool isNoise(float xVal, float yVal, float zVal, float wVal) {
    if (!isQuaternionInitialized) {isQuaternionInitialized = true; return false;}

    const float noiseThreshold = 0.95f;
    bool value = (fabsf(xVal - quaternion.x) > noiseThreshold ||
            fabsf(yVal - quaternion.y) > noiseThreshold ||
            fabsf(zVal - quaternion.z) > noiseThreshold ||
            fabsf(wVal - quaternion.w) > noiseThreshold);

    return value;
}

void updateQuaternionValues(float xVal, float yVal, float zVal, float wVal) {
    if(!isNoise(xVal, yVal, zVal, wVal)) {
        quaternion.x = xVal;
        quaternion.y = yVal;
        quaternion.z = zVal;
        quaternion.w = wVal;
    }
}

// ------------------------------------------------------------------------------------
// JNI functions
// ------------------------------------------------------------------------------------

#ifdef PLATFORM_ANDROID
JNIEXPORT void JNICALL
Java_com_example_esp32_1mpu6050_1mobile_1data_1collection_raylib_NativeBridge_updateOrientation(
        JNIEnv *env, jobject thiz, jfloat x, jfloat y, jfloat z, jfloat w) {

    // MPU MARKINGS - ACTUAL MOVEMENT OF DEVICE - ACTUAL MOVEMENT ON GRAPH
    // Y - ROLL - YAW
    // Z - YAW - PITCH
    // X - PITCH - ROLL

    /* REMAPPINGS
     * X -> Z
     * Y -> X
     * Z -> Y
     */
    updateQuaternionValues(y, z, x, w);
}
#else
void updateOrientation(void)
{
    float t = GetTime();

    float x = sinf(t * 0.5f) * 0.5f;
    float y = cosf(t * 0.4f) * 0.5f;
    float z = sinf(t * 0.3f) * 0.5f;
    float w = cosf(t * 0.2f) * 0.8f + 0.2f;

    updateQuaternionValues(y, z, x, w);
}
#endif

//----------------------------------------------------------------------------------
// Title Screen Functions Definition
//----------------------------------------------------------------------------------

// Title Screen Initialization logic
void InitVisualizerScreen(void)
{
    framesCounter = 0;
    finishScreen = 0;

    camera.position = (Vector3){ 0.0f, 15.0f, -34.0f };// Camera position perspective
    camera.target = (Vector3){ 0.0f, 0.0f, 0.0f };      // Camera looking at point
    camera.up = (Vector3){ 0.0f, 1.0f, 0.0f };          // Camera up vector (rotation towards target)

    float aspect = (float)VIRTUAL_WIDTH / (float)VIRTUAL_HEIGHT;
    camera.fovy = 25.0f / aspect;                                // Camera field-of-view Y

    camera.projection = CAMERA_PERSPECTIVE;             // Camera type

#ifdef PLATFORM_ANDROID
    model = LoadModel("modelResources/models/gltf/esp8266.glb");                  // Load model
#else
    model = LoadModel("../assets/modelResources/models/gltf/esp8266.glb");                  // Load model
#endif

    baseTransform = MatrixRotateY(DEG2RAD * -90.0f);
    // Apply the initial rotation only once (combine with model's existing transform)
    model.transform = baseTransform;

    // Shader & Texture

    crtShader = LoadShader( 0, "resources/shaders/crtShader.fs");
    timeLoc = GetShaderLocation(crtShader, "time");
    visualizerTarget = LoadRenderTexture(
        VIRTUAL_WIDTH,
        VIRTUAL_HEIGHT
    );

    // Disable texture filtering ; for pixely affect
    SetTextureFilter(visualizerTarget.texture, TEXTURE_FILTER_POINT);

    // Init Shaders

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

// Title Screen Update logic
int UpdateVisualizerScreen(void)
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

    // Press enter or tap to change to GAMEPLAY screen
    if (IsKeyPressed(KEY_ENTER))
    {
        finishScreen = 1;   // LOGO
    }

#ifndef PLATFORM_ANDROID
    updateOrientation();
#endif

    UpdateOrbitalCamera(&camera, 60.0f/60.0f);
    // UpdateCamera(&camera, CAMERA_ORBITAL);
    // ------------------ Create Transformation ------------------

    // -----------------------------------------------------------
    // QuaternionNormalize(quaternion); // was causing issue with noise detection...

    // Transformation matrix for rotations
    Matrix dynamicRotation = QuaternionToMatrix(quaternion);
    model.transform = MatrixMultiply(dynamicRotation, baseTransform);
}

// Title Screen Draw logic
void DrawVisualizerScreen(void)
{
    // Draw
    //----------------------------------------------------------------------------------
    BeginTextureMode(visualizerTarget);

    ClearBackground(WHITE);

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
    sprintf(textMessage, "Value: %f, %f, %f, %f", quaternion.x, quaternion.y, quaternion.z, quaternion.w);
    DrawText(textMessage, 20, 20, 20, WHITE);

    if (GuiButton((Rectangle){4.0f, 4.0f, 100.0f, 40.0f}, "Back")) {
        EndDrawing();
        exit(1);
    }

    EndTextureMode();

    //--------------------------------------------------------------

    BeginShaderMode(crtShader);

    // upscale to actual screen
    DrawTexturePro(
            visualizerTarget.texture,
            (Rectangle){ 0, 0,
            (float)visualizerTarget.texture.width,
            -(float)visualizerTarget.texture.height },
            (Rectangle){ 0, 0,
            (float)GetScreenWidth(),
            (float)GetScreenHeight() },
            (Vector2){0, 0},
            0,
            WHITE
            );

    EndShaderMode();
}

// Title Screen Unload logic
void UnloadVisualizerScreen(void)
{
    UnloadModel(model);     // Unload model data
}

// Title Screen should finish?
int FinishVisualizerScreen(void)
{
    return finishScreen;
}

// ------------------------------------------------------------------------------------
// My Functions
// ------------------------------------------------------------------------------------

// Basic structure for manual orbital camera
void UpdateOrbitalCamera(Camera *camera, float deltaTime) {
    static float angleX = 0.0f;  // horizontal rotation
    static float angleY = 20.0f; // vertical tilt
    static float distance = 40.0f; // zoom distance

    // Get input (mouse or touch)
    if (IsMouseButtonDown(MOUSE_BUTTON_LEFT)) {
        float dx = GetMouseDelta().x;
        float dy = GetMouseDelta().y;

        angleX += dx * 0.3f;
        angleY += dy * 0.3f;

        // Clamp vertical angle
        if (angleY > 89.0f) angleY = 89.0f;
        if (angleY < -89.0f) angleY = -89.0f;
    }

    // Zoom in/out
    float wheel = GetMouseWheelMove();
    distance -= wheel * 0.5f;
    if (distance < 2.0f) distance = 2.0f;
    if (distance > 50.0f) distance = 50.0f;

    // Convert spherical coords → cartesian
    Vector3 target = {0, 0, 0};
    camera->position.x = target.x + distance * cosf(DEG2RAD * angleY) * sinf(DEG2RAD * angleX) * -1;
    camera->position.y = target.y + distance * sinf(DEG2RAD * angleY) * -1;
    camera->position.z = target.z + distance * cosf(DEG2RAD * angleY) * cosf(DEG2RAD * angleX) * -1;

    camera->target = target;
}
