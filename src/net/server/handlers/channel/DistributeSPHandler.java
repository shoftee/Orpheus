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

import client.ISkill;
import client.GameCharacter;
import client.GameClient;
import client.MapleStat;
import client.SkillFactory;
import constants.skills.Aran;
import net.AbstractPacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public final class DistributeSPHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		slea.readInt();
		int skillid = slea.readInt();
		GameCharacter player = c.getPlayer();
		int remainingSp = player.getRemainingSp();
		boolean isBeginnerSkill = false;
		if (skillid % 10000000 > 999 && skillid % 10000000 < 1003) {
			int total = 0;
			for (int i = 0; i < 3; i++) {
				total += player.getSkillLevel(SkillFactory.getSkill(player.getJobType() * 10000000 + 1000 + i));
			}
			remainingSp = Math.min((player.getLevel() - 1), 6) - total;
			isBeginnerSkill = true;
		}
		ISkill skill = SkillFactory.getSkill(skillid);
		int curLevel = player.getSkillLevel(skill);
		if ((remainingSp > 0 && curLevel + 1 <= (skill.isFourthJob() ? player.getMasterLevel(skill) : skill.getMaxLevel()))) {
			if (!isBeginnerSkill) {
				player.setRemainingSp(player.getRemainingSp() - 1);
			}
			player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
			player.changeSkillLevel(skill, (byte) (curLevel + 1), player.getMasterLevel(skill), player.getSkillExpiration(skill));
			if (skillid == Aran.FULL_SWING) {
				ISkill hidden1 = SkillFactory.getSkill(Aran.HIDDEN_FULL_DOUBLE);
				ISkill hidden2 = SkillFactory.getSkill(Aran.HIDDEN_FULL_TRIPLE);
				player.changeSkillLevel(hidden1, (byte) (curLevel + 1), player.getMasterLevel(hidden1), player.getSkillExpiration(hidden1));
				player.changeSkillLevel(hidden2, (byte) (curLevel + 1), player.getMasterLevel(hidden2), player.getSkillExpiration(hidden2));
			} else if (skillid == Aran.OVER_SWING) {
				ISkill hidden1 = SkillFactory.getSkill(Aran.HIDDEN_OVER_DOUBLE);
				ISkill hidden2 = SkillFactory.getSkill(Aran.HIDDEN_OVER_TRIPLE);
				player.changeSkillLevel(hidden1, (byte) (curLevel + 1), player.getMasterLevel(hidden1), player.getSkillExpiration(hidden1));
				player.changeSkillLevel(hidden2, (byte) (curLevel + 1), player.getMasterLevel(hidden2), player.getSkillExpiration(hidden2));
			}
		}
	}
}
