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
package server.quest;

import java.util.Calendar;
import client.IItem;
import client.GameCharacter;
import client.InventoryType;
import client.Job;
import client.Pet;
import client.QuestCompletionState;
import client.QuestStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import provider.MapleData;
import provider.MapleDataTool;
import server.ItemInfoProvider;

/**
 * 
 * @author Matze
 */
public class QuestRequirement {
	private QuestRequirementType type;
	private MapleData data;
	private Quest quest;

	public QuestRequirement(Quest quest, QuestRequirementType type, MapleData data) {
		this.type = type;
		this.data = data;
		this.quest = quest;
	}

	boolean check(GameCharacter c, Integer npcid) {
		switch (getType()) {
			case END_DATE:
				String timeStr = MapleDataTool.getString(getData());
				Calendar cal = Calendar.getInstance();
				cal.set(Integer.parseInt(timeStr.substring(0, 4)), Integer.parseInt(timeStr.substring(4, 6)), Integer.parseInt(timeStr.substring(6, 8)), Integer.parseInt(timeStr.substring(8, 10)), 0);
				return cal.getTimeInMillis() >= System.currentTimeMillis();
			
			case FIELD_ENTER:
				MapleData zeroField = getData().getChildByPath("0");
				if (zeroField != null) {
					return MapleDataTool.getInt(zeroField) == c.getMapId();
				}
				return false;
			
			case INTERVAL:
				return !c.getQuest(quest).getCompletionState().equals(QuestCompletionState.COMPLETED) || c.getQuest(quest).getCompletionTime() <= System.currentTimeMillis() - MapleDataTool.getInt(getData()) * 60 * 1000;
			
			case ITEM:
				ItemInfoProvider ii = ItemInfoProvider.getInstance();
				for (MapleData itemEntry : getData().getChildren()) {
					int itemId = MapleDataTool.getInt(itemEntry.getChildByPath("id"));
					short quantity = 0;
					InventoryType iType = ii.getInventoryType(itemId);
					for (IItem item : c.getInventory(iType).listById(itemId))
						quantity += item.getQuantity();
					
					// Weird stuff, nexon made some quests only available when
					// wearing gm clothes. This enables us to accept it ><
					if (iType.equals(InventoryType.EQUIP)) {
						for (IItem item : c.getInventory(InventoryType.EQUIPPED).listById(itemId)) {
							quantity += item.getQuantity();
						}
					}

					if (itemEntry.getChildByPath("count") != null) {
						if (quantity < MapleDataTool.getInt(itemEntry.getChildByPath("count"), 0) || MapleDataTool.getInt(itemEntry.getChildByPath("count"), 0) <= 0 && quantity > 0) {
							return false;
						}
					} else {
						if (quantity != 0) {
							return false;
						}
					}
				}
				return true;
				
			case JOB:
				for (MapleData jobEntry : getData().getChildren()) {
					if (c.getJob().equals(Job.getById(MapleDataTool.getInt(jobEntry))) || c.isGM()) {
						return true;
					}
				}
				return false;
				
			case QUEST:
				for (MapleData questEntry : getData().getChildren()) {
					QuestStatus q = c.getQuest(Quest.getInstance(MapleDataTool.getInt(questEntry.getChildByPath("id"))));
					if (q == null && QuestCompletionState.getById(MapleDataTool.getInt(questEntry.getChildByPath("state"))).equals(QuestCompletionState.NOT_STARTED)) {
						continue;
					}
					if (q == null || !q.getCompletionState().equals(QuestCompletionState.getById(MapleDataTool.getInt(questEntry.getChildByPath("state"))))) {
						return false;
					}
				}
				return true;
				
			case MAX_LEVEL:
				return c.getLevel() <= MapleDataTool.getInt(getData());
				
			case MIN_LEVEL:
				return c.getLevel() >= MapleDataTool.getInt(getData());
				
			case MIN_PET_TAMENESS:
				Pet pet = c.getPet(0);
				if (pet == null) {
					return false;
				}
				return c.getPet(0).getCloseness() >= MapleDataTool.getInt(getData());
				
			case MOB:
				for (MapleData mobEntry : getData().getChildren()) {
					int mobId = MapleDataTool.getInt(mobEntry.getChildByPath("id"));
					int killReq = MapleDataTool.getInt(mobEntry.getChildByPath("count"));
					if (Integer.parseInt(c.getQuest(quest).getProgress(mobId)) < killReq) {
						return false;
					}
				}
				return true;
				
			case MONSTER_BOOK:
				return c.getMonsterBook().getTotalCards() >= MapleDataTool.getInt(getData());
				
			case NPC:
				return npcid == null || npcid == MapleDataTool.getInt(getData());
				
			case INFO_EX:				
				return c.getQuest(quest).getMedalProgress() >= quest.getInfoEx();
				
			case COMPLETED_QUEST:
				return c.getCompletedQuests().size() >= MapleDataTool.getInt(getData());
				
			default:
				return true;
		}
	}

	public QuestRequirementType getType() {
		return type;
	}

	private MapleData getData() {
		return data;
	}

	public List<Integer> getQuestItemsToShowOnlyIfQuestIsActivated() {
		if (type != QuestRequirementType.ITEM) {
			return null;
		}
		
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		List<Integer> delta = new ArrayList<Integer>();
		for (MapleData itemEntry : getData().getChildren()) {
			int itemId = MapleDataTool.getInt(itemEntry.getChildByPath("id"));
			if (ii.isQuestItem(itemId)) {
				delta.add(itemId);
			}
		}
		return Collections.unmodifiableList(delta);
	}
}
