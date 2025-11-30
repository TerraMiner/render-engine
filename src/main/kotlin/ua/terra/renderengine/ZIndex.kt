package ua.terra.renderengine

enum class ZIndex(val value: Int) {
    WORLD_BLOCKS(0),
    WORLD_ENTITIES(100),
    WORLD_PLAYER(200),
    WORLD_PARTICLES(300),
    WORLD_DEBUG(400),

    HUD(500),
    HUD_INVENTORY(600),
    HUD_INVENTORY_CONTENTS(700),
    HUD_INVENTORY_TEXT(800),
    HUD_TEXT(900),

    UI_BACKGROUND(1000),
    UI_OVERLAY(1500),
    UI_ELEMENTS(2000),
    UI_CONTENTS(2500),

    UI_TEXT(3000),
    UI_TOOLTIPS(4000),

    UI_SCROLLBAR(4250),
    UI_BORDER(4750),

    UI_DEBUG(5000),
    UI_CURSOR(6000);

    operator fun invoke() = value
    operator fun plus(value: Int) = this.value + value
}