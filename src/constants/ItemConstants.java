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
package constants;

import client.InventoryType;

/**
 * 
 * @author Jay Estrella
 */
public final class ItemConstants {
	public final static int LOCK = 0x01;
	public final static int SPIKES = 0x02;
	public final static int COLD = 0x04;
	public final static int UNTRADEABLE = 0x08;
	public final static int KARMA = 0x10;
	public final static int PET_COME = 0x80;
	public final static int UNKNOWN_SKILL = 0x100;
	public final static float ITEM_ARMOR_EXP = 1 / 350000;
	public static final float ITEM_WEAPON_EXP = 1 / 700000;

	public final static boolean EXPIRING_ITEMS = true;

	public static int getFlagByInt(int type) {
		if (type == 128) {
			return PET_COME;
		} else if (type == 256) {
			return UNKNOWN_SKILL;
		}
		return 0;
	}

	public static boolean isThrowingStar(int itemId) {
		return itemId / 10000 == 207;
	}

	public static boolean isBullet(int itemId) {
		return itemId / 10000 == 233;
	}

	public static boolean isRechargable(int itemId) {
		return itemId / 10000 == 233 || itemId / 10000 == 207;
	}

	public static boolean isArrowForCrossBow(int itemId) {
		return itemId / 1000 == 2061;
	}

	public static boolean isArrowForBow(int itemId) {
		return itemId / 1000 == 2060;
	}

	public static boolean isPet(int itemId) {
		return itemId / 1000 == 5000;
	}

	public static InventoryType getInventoryType(final int itemId) {
		final byte type = (byte) (itemId / 1000000);
		if (type < 1 || type > 5) {
			return InventoryType.UNDEFINED;
		}
		return InventoryType.fromByte(type);
	}

	public static boolean isOverall(int itemId) {
		return itemId / 10000 == 105;
	}

	public static boolean isWeapon(int itemId) {
		return itemId >= 1302000 && itemId < 1492024;
	}
}
