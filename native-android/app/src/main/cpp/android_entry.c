#include <SDL.h>
#include <SDL_system.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <errno.h>

int lotd_host_main(int argc, char **argv);
int SDL_main(int argc, char **argv) {
    (void)argc; (void)argv;
    const char *external = SDL_AndroidGetExternalStoragePath();
    const char *internal = SDL_AndroidGetInternalStoragePath();
    if (!external || !internal) {
        __android_log_print(ANDROID_LOG_ERROR, "LOTD-NATIVE", "SDL Android storage paths unavailable");
        return 2;
    }
    char game[1024], save[1024];
    snprintf(game, sizeof(game), "%s/game", external);
    snprintf(save, sizeof(save), "%s/save", internal);
    mkdir(save, 0700);
    /* LOTD_STAGE7_GAME_CWD_FIX: the native runtime resolves System/LOTD.exe
     * relative to its working directory. Asset files were pushed to files/game. */
    char check_exe[1200];
    struct stat check_stat;
    const char *selected_game = game;
    snprintf(check_exe, sizeof(check_exe), "%s/System/LOTD.exe", selected_game);
    if (stat(check_exe, &check_stat) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "LOTD-NATIVE",
                            "Engine game file not found: %s (errno=%d)", check_exe, errno);
        /* Alternate spelling of the same app-specific external directory. */
        const char *fallback_game = "/sdcard/Android/data/dev.popeyeplay.lotdengine/files/game";
        snprintf(check_exe, sizeof(check_exe), "%s/System/LOTD.exe", fallback_game);
        if (stat(check_exe, &check_stat) == 0) {
            selected_game = fallback_game;
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "LOTD-NATIVE",
                                "Fallback game file not found: %s (errno=%d)", check_exe, errno);
        }
    }
    if (chdir(selected_game) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "LOTD-NATIVE",
                            "Cannot enter game folder: %s (errno=%d)", selected_game, errno);
        chdir(external); /* Store failure log in original app files directory. */
    } else {
        __android_log_print(ANDROID_LOG_INFO, "LOTD-NATIVE",
                            "Working directory changed to: %s", selected_game);
    }
    __android_log_print(ANDROID_LOG_INFO, "LOTD-NATIVE", "game folder: %s", game);
    __android_log_print(ANDROID_LOG_INFO, "LOTD-NATIVE", "save folder: %s", save);
    __android_log_print(ANDROID_LOG_INFO, "LOTD-NATIVE", "This is an engine launch test; original game files are NOT embedded");
    char *args[] = {"lotd", game, save, NULL};
    /* LOTD_STAGE9_FILE_TRACE: record each guest CreateFile / FindFirstFile lookup. */
    setenv("PORT_LOGFILES", "1", 1);
    __android_log_print(ANDROID_LOG_INFO, "LOTD-NATIVE", "PORT_LOGFILES enabled");
    return lotd_host_main(3, args);
}


/* LOTD_STAGE12_JNI_TOUCH_EVENT_BRIDGE:
 * Java HUD only enqueues SDL user events (thread safe). user32.c consumes
 * these on the game's own SDL event pump. No guest memory touched here. */
#include <jni.h>
#include <stdint.h>
JNIEXPORT void JNICALL
Java_org_libsdl_app_LOTDTouchOverlay_nativeSend(JNIEnv *env, jclass clazz,
                                                jint operation, jint arg1, jint arg2) {
    (void)env; (void)clazz;
    if (operation < 1 || operation > 3) return;
    SDL_Event e;
    SDL_zero(e);
    e.type = SDL_USEREVENT;
    e.user.type = SDL_USEREVENT;
    e.user.code = 0x4c5400 + (int)operation;
    e.user.data1 = (void *)(intptr_t)arg1;
    e.user.data2 = (void *)(intptr_t)arg2;
    SDL_PushEvent(&e);
}
