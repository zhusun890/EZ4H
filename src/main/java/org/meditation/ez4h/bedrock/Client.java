package org.meditation.ez4h.bedrock;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.v408.Bedrock_v408;
import io.netty.util.AsciiString;
import org.meditation.ez4h.Variables;
import org.meditation.ez4h.mcjava.ClientHandler;
import org.meditation.ez4h.mcjava.ClientStat;
import org.meditation.ez4h.utils.RandUtils;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.UUID;

public class Client {
    public BedrockClientSession session;
    public Session JESession;
    public String playerName;
    public String xuid;
    public UUID playerUUID;
    public ClientHandler javaHandler;
    public ClientStat clientStat;
    public Client(PacketReceivedEvent event, String playerName, UUID playerUUID){
        this.playerName=playerName;
        this.playerUUID=playerUUID;
        Client clientM=this;
        try {
            JESession=event.getSession();
            this.clientStat=new ClientStat();
            this.javaHandler=new ClientHandler(clientM);
            InetSocketAddress bindAddress = new InetSocketAddress("0.0.0.0", RandUtils.rand(10000,50000));
            BedrockClient client = new BedrockClient(bindAddress);
            client.bind().join();
            InetSocketAddress addressToConnect = new InetSocketAddress(Variables.config.getString("be_host"), Variables.config.getInteger("be_port"));
            client.connect(addressToConnect).whenComplete((session, throwable) -> {
                if (throwable != null) {
                    return;
                }
                this.session=session;
                session.setPacketCodec(Bedrock_v408.V408_CODEC);
                session.addDisconnectHandler((reason) -> {
                    event.getSession().disconnect("Raknet Disconnect!Please Check your bedrock server!");
                });
                session.setPacketHandler(new BedrockHandler(clientM));
                this.startLogin(session);
            }).join();
        } catch (Exception e) {
            event.getSession().disconnect("EZ4H ERROR!\nCaused by "+e.getLocalizedMessage());
            if(session != null){
                session.disconnect();
            }
            e.printStackTrace();
        }
    }
    public void startLogin(BedrockClientSession session){
        LoginPacket loginPacket=new LoginPacket();
        loginPacket.setProtocolVersion(Variables.config.getJSONObject("advanced").getInteger("be_protocol"));
        JSONObject chain=new JSONObject(),chain1=new JSONObject(),extraData=new JSONObject();
        JSONArray chainL=new JSONArray();
        extraData.put("displayName",this.playerName);
        extraData.put("identity",this.playerUUID.toString());
        extraData.put("XUID",this.xuid);
        chain1.put("extraData",extraData);
        chainL.add("."+ Base64.getEncoder().encodeToString(chain1.toJSONString().getBytes()) +".");
        chain.put("chain",chainL);
        loginPacket.setChainData(new AsciiString(chain.toJSONString()));
        JSONObject skinData=new JSONObject();
        skinData.put("ClientRandomId",RandUtils.rand(0,9999999));
        skinData.put("SkinId",this.playerName.replaceAll(" ","_").toLowerCase()+"_skin");
        skinData.put("CapeId",this.playerName.replaceAll(" ","_").toLowerCase()+"_cape");
        skinData.put("SkinData",BedrockUtils.skinTextureToString(new File("./resources/skin.png")));
        skinData.put("SkinImageWidth",64);
        skinData.put("SkinImageHeight",64);
        skinData.put("SkinGeometryData", Base64.getEncoder().encodeToString(Variables.SKIN_GEOMETRY_DATA.getBytes()));
        skinData.put("SkinResourcePatch",Base64.getEncoder().encodeToString("{\"geometry\" : {\"default\" : \"geometry.humanoid.custom\"}}".getBytes()));
        loginPacket.setSkinData(new AsciiString("."+Base64.getEncoder().encodeToString(skinData.toJSONString().getBytes())+"."));
        session.sendPacket(loginPacket);
    }
    public void sendMessage(String msg){
        this.JESession.send(new ServerChatPacket(msg));
    }
    public void sendAlert(String msg){
        this.JESession.send(new ServerChatPacket("§f[§bEZ§a4§bH§f]"+msg));
    }
}
