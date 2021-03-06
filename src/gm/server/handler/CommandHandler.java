/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss <aaron@deviant-core.net>
    				Patrick Huy <patrick.huy@frz.cc>
					Matthias Butz <matze@odinms.de>
					Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package gm.server.handler;

import client.GameCharacter;
import gm.GMPacketCreator;
import gm.GMPacketHandler;
import net.server.Server;
import net.server.World;
import org.apache.mina.core.session.IoSession;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author kevintjuh93
 */
public class CommandHandler implements GMPacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor reader, IoSession session) {
		byte command = reader.readByte();
		switch (command) {
			case 0: {// notice
				for (World world : Server.getInstance().getWorlds()) {
					world.broadcastPacket(PacketCreator.serverNotice(0, reader.readMapleAsciiString()));
				}
				break;
			}
			
			case 1: {// server message
				for (World world : Server.getInstance().getWorlds()) {
					world.setServerMessage(reader.readMapleAsciiString());
				}
				break;
			}
			
			case 2: {
				Server server = Server.getInstance();
				byte worldId = reader.readByte();
				if (worldId >= server.getWorlds().size()) {
					session.write(GMPacketCreator.commandResponse((byte) 2));
					return;// incorrect world
				}
				World world = server.getWorld(worldId);
				switch (reader.readByte()) {
					case 0:
						world.getRates().exp(reader.readByte());
						break;
					case 1:
						world.getRates().drop(reader.readByte());
						break;
					case 2:
						world.getRates().meso(reader.readByte());
						break;
				}
				for (GameCharacter chr : world.getPlayerStorage().getAllCharacters()) {
					chr.refreshRates();
				}
			}
			
			case 3: {
				String user = reader.readMapleAsciiString();
				for (World world : Server.getInstance().getWorlds()) {
					if (world.isConnected(user)) {
						world.getPlayerStorage().getCharacterByName(user).getClient().disconnect();
						session.write(GMPacketCreator.commandResponse((byte) 1));
						return;
					}
				}
				session.write(GMPacketCreator.commandResponse((byte) 0));
				break;
			}
			
			case 4: {
				String user = reader.readMapleAsciiString();
				for (World world : Server.getInstance().getWorlds()) {
					if (world.isConnected(user)) {
						GameCharacter chr = world.getPlayerStorage().getCharacterByName(user);
						chr.ban(reader.readMapleAsciiString());
						chr.sendPolice("You have been blocked by #b" + session.getAttribute("NAME") + " #kfor the HACK reason.");
						session.write(GMPacketCreator.commandResponse((byte) 1));
						return;
					}
				}
				session.write(GMPacketCreator.commandResponse((byte) 0));
				break;
			}
			
			case 5: {
				String user = reader.readMapleAsciiString();
				for (World world : Server.getInstance().getWorlds()) {
					if (world.isConnected(user)) {
						GameCharacter chr = world.getPlayerStorage().getCharacterByName(user);
						String job = chr.getJob().name() + " (" + chr.getJob().getId() + ")";
						session.write(GMPacketCreator.playerStats(user, job, (byte) chr.getLevel(), chr.getExp(), (short) chr.getMaxHp(), (short) chr.getMaxMp(), (short) chr.getStr(), (short) chr.getDex(), (short) chr.getInt(), (short) chr.getLuk(), chr.getMeso()));
						return;
					}
				}
				session.write(GMPacketCreator.commandResponse((byte) 0));
			}
			
			case 7: {
				Server.getInstance().shutdown(false).run();
			}
			
			case 8: {
				Server.getInstance().shutdown(true).run();
			}
		}
	}

}
