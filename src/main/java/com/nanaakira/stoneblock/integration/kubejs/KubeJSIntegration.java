package com.nanaakira.stoneblock.integration.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;

/**
 * StoneBlock 的 KubeJS 集成。
 * 把 SpawnerDataKjs 绑定为全局变量 stoneblockEntitiesData，供 startup_scripts 使用。
 */
public class KubeJSIntegration extends KubeJSPlugin {
    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("stoneblockEntitiesData", SpawnerDataKjs.class);
    }
}
