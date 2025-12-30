package com.example.gamelabs.util

import android.view.InputDevice
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key

import androidx.compose.ui.input.key.type
import com.example.gamelabs.model.RetroInput
const val KEYCODE_BUTTON_A = 96
const val KEYCODE_BUTTON_B = 97
const val KEYCODE_BUTTON_X = 99
const val KEYCODE_BUTTON_Y = 100
const val KEYCODE_BUTTON_L1 = 102
const val KEYCODE_BUTTON_R1 = 103
const val KEYCODE_BUTTON_L2 = 104
const val KEYCODE_BUTTON_R2 = 105
const val KEYCODE_BUTTON_SELECT = 109
const val KEYCODE_BUTTON_START = 108
const val KEYCODE_DPAD_CENTER = 23

/**
 * Verifica se o evento veio de um Gamepad ou Joystick.
 */
fun KeyEvent.isFromJoystick(): Boolean {
    val source = this.nativeKeyEvent.device?.sources ?: return false
    return (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
}

/**
 * Lógica unificada para confirmar (Botão A, Enter, Start, Dpad Center)
 */
fun KeyEvent.isConfirmButton(): Boolean {
    if (this.type != KeyEventType.KeyUp) return false

    val nativeCode = this.nativeKeyEvent.keyCode
    return when (nativeCode) {
        KEYCODE_BUTTON_A,
        KEYCODE_DPAD_CENTER,
        KEYCODE_BUTTON_START,
        android.view.KeyEvent.KEYCODE_ENTER, // Uso explícito para evitar conflito
        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> true
        else -> this.key == Key.Enter || this.key == Key.NumPadEnter || this.key == Key.DirectionCenter
    }
}

/**
 * Lógica unificada para voltar (Botão B, Backspace, Esc, Back nativo)
 */
fun KeyEvent.isBackButton(): Boolean {
    if (this.type != KeyEventType.KeyUp) return false

    val nativeCode = this.nativeKeyEvent.keyCode
    return when (nativeCode) {
        KEYCODE_BUTTON_B,
        android.view.KeyEvent.KEYCODE_BACK, // Uso explícito
        android.view.KeyEvent.KEYCODE_ESCAPE -> true
        else -> this.key == Key.Back || this.key == Key.Escape
    }
}

// --- Botões de Ombro (L1/R1) ---

fun KeyEvent.isL1(): Boolean {
    return this.type == KeyEventType.KeyDown &&
            this.nativeKeyEvent.keyCode == KEYCODE_BUTTON_L1
}

fun KeyEvent.isR1(): Boolean {
    return this.type == KeyEventType.KeyDown &&
            this.nativeKeyEvent.keyCode == KEYCODE_BUTTON_R1
}

fun KeyEvent.isL2(): Boolean {
    return this.type == KeyEventType.KeyDown &&
            this.nativeKeyEvent.keyCode == KEYCODE_BUTTON_L2
}

fun KeyEvent.isR2(): Boolean {
    return this.type == KeyEventType.KeyDown &&
            this.nativeKeyEvent.keyCode == KEYCODE_BUTTON_R2
}

// --- Direcionais ---

fun KeyEvent.isDpadRight(): Boolean {
    return this.type == KeyEventType.KeyDown &&
            (this.key == Key.DirectionRight || this.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT)
}

fun KeyEvent.isDpadLeft(): Boolean {
    return this.type == KeyEventType.KeyDown &&
            (this.key == Key.DirectionLeft || this.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT)
}

fun KeyEvent.isDpadUp(): Boolean {
    return this.type == KeyEventType.KeyDown &&
            (this.key == Key.DirectionUp || this.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP)
}

fun KeyEvent.isDpadDown(): Boolean {
    return this.type == KeyEventType.KeyDown &&
            (this.key == Key.DirectionDown || this.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN)
}

fun mapKeyCodeToRetroId(keyCode: Int): Int? {
    return when (keyCode) {
        // Direcionais
        android.view.KeyEvent.KEYCODE_DPAD_UP -> RetroInput.ID_UP
        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> RetroInput.ID_DOWN
        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> RetroInput.ID_LEFT
        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> RetroInput.ID_RIGHT

        // Botões de Ação (Mapeamento Físico -> Lógica SNES)
        // O Android A (baixo) vira o B do SNES (baixo)
        KEYCODE_BUTTON_A -> RetroInput.ID_B
        // O Android B (direita) vira o A do SNES (direita)
        KEYCODE_BUTTON_B -> RetroInput.ID_A
        // O Android X (esquerda) vira o Y do SNES (esquerda)
        KEYCODE_BUTTON_X -> RetroInput.ID_Y
        // O Android Y (cima) vira o X do SNES (cima)
        KEYCODE_BUTTON_Y -> RetroInput.ID_X

        // Start / Select
        KEYCODE_BUTTON_START -> RetroInput.ID_START
        KEYCODE_BUTTON_SELECT -> RetroInput.ID_SELECT

        // Ombros
        KEYCODE_BUTTON_L1 -> RetroInput.ID_L
        KEYCODE_BUTTON_R1 -> RetroInput.ID_R
        KEYCODE_BUTTON_L2 -> RetroInput.ID_L2
        KEYCODE_BUTTON_R2 -> RetroInput.ID_R2

        else -> null
    }
}