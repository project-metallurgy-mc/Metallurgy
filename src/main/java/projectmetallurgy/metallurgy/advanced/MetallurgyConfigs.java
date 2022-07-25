package projectmetallurgy.metallurgy.advanced;

import net.minecraftforge.common.ForgeConfigSpec;
import projectmetallurgy.metallurgy.block.copper.BlockChalcopyrite;
import projectmetallurgy.metallurgy.block.copper.BlockMalachite;
import projectmetallurgy.metallurgy.block.iron.BlockHematite;

public class MetallurgyConfigs {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG_SPEC;

    static {
        BUILDER.push("Ores");

        BUILDER.push("Eta");
        BlockChalcopyrite.Eta0 = BUILDER.comment("Chalcopyrite Eta0").define("chalcopyrite_eta0", 0.8);
        BlockMalachite.Eta0 = BUILDER.comment("Malachite Eta0").define("malachite_eta0", 0.75);
        BlockHematite.Eta0 = BUILDER.comment("Hematite Eta0").define("hematite_eta0", 0.9);
        BUILDER.pop();

        BUILDER.push("Gravity separation particle size");
        BlockChalcopyrite.Gsps = BUILDER.comment("Chalcopyrite Gsps").define("chalcopyrite_gsps", 1000);
        BlockMalachite.Gsps = BUILDER.comment("Malachite Gsps").define("malachite_gsps", 1000);
        BlockHematite.Gsps = BUILDER.comment("Hematite Gsps").define("hematite_gsps", 1000);
        BUILDER.pop();

        BUILDER.push("Can manual separation");
        BlockChalcopyrite.Manual = BUILDER.comment("Chalcopyrite").define("chalcopyrite_manual", false);
        BlockMalachite.Manual = BUILDER.comment("Malachite").define("malachite_manual", true);
        BlockHematite.Manual = BUILDER.comment("Hematite").define("hematite_manual", true);
        BUILDER.pop();

        BUILDER.push("Alpha");
        BlockChalcopyrite.Alpha = BUILDER.comment("Chalcopyrite Alpha").define("chalcopyrite_alpha", 0.0);
        BlockMalachite.Alpha = BUILDER.comment("Malachite Alpha").define("malachite_alpha", 0.0);
        BlockHematite.Alpha = BUILDER.comment("Hematite Alpha").define("hematite_alpha", 0.0);
        BUILDER.pop();

        BUILDER.pop();

        BUILDER.push("Processing");

        BUILDER.push("Stone hammer and anvil");
        BUILDER.comment("Loss rate [0,1]").define("shaa_lossrate",0.04);
        BUILDER.comment("Minimum particle size [10,100000] 100000 to disable.").define("shaa_mps",1000);
        BUILDER.comment("Crushing rate [0,1]").define("shaa_cr",0.5);
        BUILDER.pop();

        BUILDER.push("Stone mortar and pestle");
        BUILDER.comment("Minimum particle size [10,100000] 100000 to disable.").define("smap_mps",50);
        BUILDER.comment("Crushing rate [0,1]").define("smap_cr",0.8);
        BUILDER.comment("Veinstone prefix [0,intmax]").define("smap_vp",100);
        BUILDER.pop();

        BUILDER.pop();

        CONFIG_SPEC = BUILDER.build();
    }
}
