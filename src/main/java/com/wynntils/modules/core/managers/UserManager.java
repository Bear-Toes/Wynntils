/*
 *  * Copyright © Wynntils - 2020.
 */

package com.wynntils.modules.core.managers;

import com.google.gson.JsonObject;
import com.wynntils.modules.core.enums.AccountType;
import com.wynntils.modules.core.instances.account.CosmeticInfo;
import com.wynntils.modules.core.instances.account.WynntilsUser;
import com.wynntils.webapi.WebManager;
import com.wynntils.webapi.request.PostRequest;
import com.wynntils.webapi.request.Request;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.UUID;

public class UserManager {

    private static HashMap<UUID, WynntilsUser> users = new HashMap<>();

    public static void loadUser(UUID uuid) {
        if (users.containsKey(uuid)) return;
        users.put(uuid, null); // temporary null, avoid extra loads

        JsonObject body = new JsonObject();
        body.addProperty("uuid", uuid.toString());

        Request req = new PostRequest(WebManager.getApiUrls().get("Athena") + "/user/getInfo", "getInfo(" + uuid.toString() + ")")
                .postJsonElement(body)
                .handleJsonObject(json -> {
                    if (!json.has("user")) return false;

                    JsonObject user = json.getAsJsonObject("user");
                    ResourceLocation rl = new ResourceLocation("wynntils:capes/" + uuid.toString().replace("-", ""));

                    JsonObject cosmetics = user.getAsJsonObject("cosmetics");
                    CosmeticInfo info = new CosmeticInfo(
                            cosmetics.get("hasEars").getAsBoolean(),
                            cosmetics.get("hasCape").getAsBoolean(),
                            cosmetics.get("hasElytra").getAsBoolean(),
                            cosmetics.get("texture").getAsString(),
                            rl
                    );

                    // loading
                    TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                    textureManager.loadTexture(rl, info);

                    users.put(uuid, new WynntilsUser(AccountType.valueOf(user.get("accountType").getAsString()), info));
                    return true;
                }).onError(i -> false);

        WebManager.getHandler().addAndDispatch(req, true);
    }

    public static WynntilsUser getUser(UUID uuid) {
        return users.getOrDefault(uuid, null);
    }

    public static boolean isAccountType(UUID uuid, AccountType type) {
        return users.containsKey(uuid) && users.get(uuid) != null && users.get(uuid).getAccountType() == type;
    }

    public static void clearRegistry() {
        users.clear();
    }

}
