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
package net.server.handlers.login;

import client.GameClient;
import constants.ServerConstants;
import net.AbstractPacketHandler;
import net.server.Server;
import net.server.World;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ServerlistRequestHandler extends AbstractPacketHandler {

	private static final String[] names = ServerConstants.WORLD_NAMES;
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		Server server = Server.getInstance();
		World world;
		for (byte i = 0; i < Math.min(server.getLoad().size(), names.length); i++) {
			world = server.getWorld(i);
			c.announce(PacketCreator.getServerList(i, names[i], world.getFlag(), world.getEventMessage(), server.getLoad(i)));
		}
		c.announce(PacketCreator.getEndOfServerList());
		// too lazy to make a check lol
		c.announce(PacketCreator.selectWorld(0));
		c.announce(PacketCreator.sendRecommended(server.worldRecommendedList()));
	}
}