package com.nanaakira.stoneblock.worldgen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Stores information about a prebuilt island structure.
 */
public class PrebuiltStructure {
    @SuppressWarnings("deprecation")
    public static final ResourceLocation DEFAULT_IMAGE = new ResourceLocation("stoneblock", "textures/default_start.png");

    public final ResourceLocation id;
    public final Component name;
    public final String author;
    public ResourceLocation image;

    @SuppressWarnings("deprecation")
    public PrebuiltStructure(ResourceLocation id, Component name, String author) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.image = new ResourceLocation(id.getNamespace(), "stoneblock_start/" + id.getPath() + ".png");
    }

    @Override
    public String toString() {
        return "PrebuiltStructure{" +
                "id=" + id +
                ", name=" + name +
                ", author='" + author + '\'' +
                ", image=" + image +
                '}';
    }
}