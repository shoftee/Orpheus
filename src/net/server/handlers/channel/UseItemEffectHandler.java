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
package net.server.handlers.channel;

import client.IItem;
import client.GameClient;
import client.InventoryType;
import net.AbstractPacketHandler;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class UseItemEffectHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		IItem toUse;
		int itemId = slea.readInt();
		if (itemId == 4290001 || itemId == 4290000) {
			toUse = c.getPlayer().getInventory(InventoryType.ETC).findById(itemId);
		} else {
			toUse = c.getPlayer().getInventory(InventoryType.CASH).findById(itemId);
		}
		if (toUse == null || toUse.getQuantity() < 1) {
			if (itemId != 0)
				return;
		}
		c.getPlayer().setItemEffect(itemId);
		c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PacketCreator.itemEffect(c.getPlayer().getId(), itemId), false);
	}
}
