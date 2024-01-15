package org.yirankuma.npcskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.neteasemc.spigotmaster.SpigotMaster;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v503.Bedrock_v503;
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class NPCSkin extends JavaPlugin implements Listener {
    private static NPCSkin instance;
    private final Map<NPC, ImageData> skinCache = new HashMap<>();

    public SpigotMaster spigotMaster;

    @Override
    public void onEnable() {
        spigotMaster = (SpigotMaster) getServer().getPluginManager().getPlugin("SpigotMaster");
        instance = this;

        getServer().getPluginManager().registerEvents(this, this);
        // Plugin startup logic

        //创建插件文件夹
        createPluginFolders();

    }

    public void createPluginFolders() {
        // 获取插件的数据文件夹
        File dataFolder = this.getDataFolder();

        // 如果数据文件夹不存在，则创建它
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        // 创建一个新的File实例，指向"dataFolder/skins"
        File skinsFolder = new File(dataFolder, "skins");

        // 如果skins文件夹不存在，则创建它
        if (!skinsFolder.exists()) {
            skinsFolder.mkdir();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (Objects.equals(label, "npcskin")) {
            if (sender instanceof Player) {

//                PlayerListPacket playerListPacket = new PlayerListPacket();
//                playerListPacket.setAction(PlayerListPacket.Action.ADD);
//                for (Player player : getServer().getOnlinePlayers()) {
//
//                    if (player != (Player) sender) {
//
//                        File skinFile = new File(getDataFolder(), "skins/test/skin.png");
//                        File geometryFile = new File(getDataFolder(), "skins/test/geometry.json");
//                        File skinResourcePatch = new File(getDataFolder(), "skins/test/skinResourcePatch.json");
//
//                        BufferedImage skinImage = null;
//                        ImageData skinData = null;
//
//                        String geometryData = "";
//                        String skinResourcePatchData = "";
//                        try {
//                            skinImage = ImageIO.read(skinFile);
//                            skinData = ImageData.from(skinImage);
//
//                            geometryData = new String(Files.readAllBytes(geometryFile.toPath()));
//                            skinResourcePatchData = new String(Files.readAllBytes(skinResourcePatch.toPath()));
//
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(player.getUniqueId());
//
//                        SerializedSkin.Builder builder = SerializedSkin.builder();
//                        builder.skinId(player.getUniqueId().toString());
//                        builder.persona(true);
//                        builder.fullSkinId("new");
//                        builder.skinColor("#0");
//                        builder.skinResourcePatch(skinResourcePatchData);
//                        builder.animationData("");
//                        builder.animations(new ArrayList<>());
//                        builder.armSize("wide");
//                        builder.capeData(skinData);
//                        builder.capeOnClassic(false);
//                        builder.personaPieces(new ArrayList<>());
//                        builder.capeId("");
//                        builder.playFabId(player.getName());
//                        builder.tintColors(new ArrayList<>());
//                        builder.geometryData(geometryData);
//                        builder.skinData(skinData);
//                        builder.premium(false);
//                        builder.geometryDataEngineVersion("1.12.0");
//
//                        SerializedSkin skin = builder.build();
//                        entry.setSkin(skin);
//                        entry.setEntityId(player.getEntityId());
//                        entry.setTrustedSkin(true);
//                        entry.setTeacher(true);
//                        entry.setHost(true);
//
//                        if (entry.getName() == null) {
//                            entry.setName(player.getName());
//                        }
//                        if (entry.getXuid() == null) {
//                            entry.setXuid("");
//                        }
//                        if (entry.getPlatformChatId() == null) {
//                            entry.setPlatformChatId("");
//                        }
//
//                        playerListPacket.getEntries().add(entry);
//
//                    }
//                }
//                System.out.println(playerListPacket.getEntries());
//                int packetId = Bedrock_v503.CODEC.getPacketDefinition(playerListPacket.getClass()).getId();
//                spigotMaster.sendBedrockPacket(packetId, encodeBedrockPacket(Bedrock_v503.CODEC, playerListPacket).getData());
                RestoreSkins((Player) sender);
                return true;
            }
        }
        return super.onCommand(sender, command, label, args);
    }

    public void RestoreSkins(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                SkinTrait skinTrait = npc.getTraitNullable(SkinTrait.class);
                if (skinTrait == null) continue;
                String tex = skinTrait.getTexture();
                if (tex == null) continue;

                ImageData skin;
                if (skinCache.containsKey(npc)) skin = skinCache.get(npc);
                else try {
                    skin = DownloadSkin(tex);
                    skinCache.put(npc, skin);
                } catch (IOException e) {
                    getLogger().severe(String.format("An error occured whilst downloading the skin for npc %s:", npc.getName()));
                    e.printStackTrace();
                    return;
                }

                SendSkinPacket(player,npc.getEntity(), skin);
            }
        });
    }

    public static String GetSkinDownloadUrl(String tex) {
        if (tex == null) return null;

        // decode
        String jsonStr = new String(Base64.getDecoder().decode(tex));
        JsonObject data = new Gson().fromJson(jsonStr, JsonObject.class);

        return data
                .get("textures").getAsJsonObject()
                .get("SKIN").getAsJsonObject()
                .get("url").getAsString();
    }

    /**
     * BLOCKING. Downloads a skin from the texture string of a player's skin data.
     *
     * @param tex texture string.
     * @return The downloaded skin in nukkit's ImageData form.
     */
    public ImageData DownloadSkin(String tex) throws IOException {
        URL url = new URL(GetSkinDownloadUrl(tex));
        BufferedImage skinImage = ImageIO.read(url);
        return ImageData.from(skinImage);
    }

    public void SendSkinPacket(Player player,Entity entity, ImageData skinData) {
        PlayerListPacket playerListPacket = new PlayerListPacket();
        playerListPacket.setAction(PlayerListPacket.Action.ADD);

        File geometryFile = new File(getDataFolder(), "skins/test/geometry.json");
        File skinResourcePatch = new File(getDataFolder(), "skins/test/skinResourcePatch.json");

        String geometryData = "";
        String skinResourcePatchData = "";
        try {
            geometryData = new String(Files.readAllBytes(geometryFile.toPath()));
            skinResourcePatchData = new String(Files.readAllBytes(skinResourcePatch.toPath()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(entity);
        System.out.println(entity.getUniqueId());
        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(entity.getUniqueId());

        SerializedSkin.Builder builder = SerializedSkin.builder();
        builder.skinId(UUID.randomUUID().toString());
        builder.persona(true);
        builder.fullSkinId("");
        builder.skinColor("");
        builder.skinResourcePatch(skinResourcePatchData);
        builder.animationData("");
        builder.animations(new ArrayList<>());
        builder.armSize("wide");
        builder.capeData(skinData);
        builder.capeOnClassic(false);
        builder.personaPieces(new ArrayList<>());
        builder.capeId("");
        builder.playFabId("");
        builder.tintColors(new ArrayList<>());
        builder.geometryData(geometryData);
        builder.skinData(skinData);
        builder.premium(false);
        builder.geometryDataEngineVersion("");

        SerializedSkin skin = builder.build();
        entry.setSkin(skin);
        entry.setEntityId(entity.getEntityId());
        entry.setTrustedSkin(true);
        entry.setTeacher(true);
        entry.setHost(true);

        if (entry.getName() == null) {
            entry.setName(entity.getName());
        }
        if (entry.getXuid() == null) {
            entry.setXuid("");
        }
        if (entry.getPlatformChatId() == null) {
            entry.setPlatformChatId("");
        }

        playerListPacket.getEntries().add(entry);
//                System.out.println(playerListPacket.getEntries());
        int packetId = Bedrock_v503.CODEC.getPacketDefinition(playerListPacket.getClass()).getId();
        spigotMaster.sendBedrockPacketToPlayer(packetId, encodeBedrockPacket(Bedrock_v503.CODEC, playerListPacket).getData(),player);
    }

    private static final ThreadLocal<ByteBuf> BYTE_BUF_THREAD_LOCAL = ThreadLocal.withInitial(() -> ByteBufAllocator.DEFAULT.heapBuffer(2024));

    public static EncodedPacketData encodeBedrockPacket(BedrockCodec codec, BedrockPacket packet) {
        ByteBuf byteBuf = BYTE_BUF_THREAD_LOCAL.get();
        byteBuf.clear(); // 清空缓冲区以便重用
        try {
            codec.tryEncode(codec.createHelper(), byteBuf, packet);
            int dataLen = byteBuf.readableBytes();
            byte[] data = new byte[dataLen];
            byteBuf.readBytes(data, 0, dataLen);
            int packetId = codec.getPacketDefinition(packet.getClass()).getId();
            return new EncodedPacketData(packetId, data);
        } finally {
            byteBuf.clear(); // 清空缓冲区以便下次使用
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}