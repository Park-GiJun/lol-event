package com.gijun.main.domain.model.dragon

/**
 * 룬 하나. DataDragon 의 runesReforged 를 그대로 옮긴 것이다.
 *
 * 계열(정밀·지배·마법·결의·영감) 자체도 같은 모델로 담는다. 그 경우
 * [runeId] 와 [styleId] 가 같고 [slot] 이 [STYLE_SLOT] 이다.
 * 화면에서 계열 아이콘을 띄우려면 계열도 id 로 찾을 수 있어야 하기 때문이다.
 */
data class DragonRune(
    val runeId: Int,
    val runeKey: String,
    val nameKo: String,
    val description: String?,
    val iconPath: String?,
    val imageUrl: String?,
    val styleId: Int,
    val styleNameKo: String?,
    /** 계열 안에서의 줄 번호. 0 이 핵심 룬(키스톤), 계열 행은 [STYLE_SLOT]. */
    val slot: Int,
    val version: String?,
) {
    val isKeystone: Boolean get() = slot == KEYSTONE_SLOT
    val isStyle: Boolean get() = slot == STYLE_SLOT

    companion object {
        const val KEYSTONE_SLOT = 0
        const val STYLE_SLOT = -1
    }
}
