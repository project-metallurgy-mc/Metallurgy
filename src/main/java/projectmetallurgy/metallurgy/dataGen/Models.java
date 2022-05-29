package projectmetallurgy.metallurgy.dataGen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeRecipeProvider;

public class Models extends ItemModelProvider{
    public Models(DataGenerator generator, String modid, ExistingFileHelper existingFileHelper) {
        super(generator, modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {
    }
}
