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
package net.server.guild;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import client.GameCharacter;
import client.GameClient;
import java.util.LinkedList;
import tools.DatabaseConnection;
import tools.Output;
import net.GamePacket;
import net.server.Channel;
import net.server.Server;
import tools.PacketCreator;

public class Guild {
	public final static int CREATE_GUILD_COST = 1500000;
	public final static int CHANGE_EMBLEM_COST = 5000000;

	private enum BCOp {
		NONE, DISBAND, EMBELMCHANGE
	}

	private List<GuildCharacter> members;
	private String rankTitles[] = new String[5]; // 1 = master, 2 = jr, 5 =
													// lowest member
	private String name, notice;
	private int id, gp, logo, logoColor, leader, capacity, logoBG, logoBGColor,
			signature, allianceId;
	private byte world;
	private Map<Byte, List<Integer>> notifications = new LinkedHashMap<Byte, List<Integer>>();
	private boolean bDirty = true;

	public Guild(GuildCharacter initiator) {
		int guildid = initiator.getGuildId();
		world = initiator.getWorld();
		members = new ArrayList<GuildCharacter>();
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `guilds` WHERE `guildid` = " + guildid);
			ResultSet rs = ps.executeQuery();
			if (!rs.first()) {
				id = -1;
				ps.close();
				rs.close();
				return;
			}
			id = guildid;
			name = rs.getString("name");
			gp = rs.getInt("GP");
			logo = rs.getInt("logo");
			logoColor = rs.getInt("logoColor");
			logoBG = rs.getInt("logoBG");
			logoBGColor = rs.getInt("logoBGColor");
			capacity = rs.getInt("capacity");
			for (int i = 1; i <= 5; i++) {
				rankTitles[i - 1] = rs.getString("rank" + i + "title");
			}
			leader = rs.getInt("leader");
			notice = rs.getString("notice");
			signature = rs.getInt("signature");
			allianceId = rs.getInt("allianceId");
			ps.close();
			rs.close();
			ps = con.prepareStatement("SELECT `id`, `name`, `level`, `job`, `guildrank`, `allianceRank` FROM `characters` WHERE `guildid` = ? ORDER BY `guildrank` ASC, `name` ASC");
			ps.setInt(1, guildid);
			rs = ps.executeQuery();
			if (!rs.first()) {
				rs.close();
				ps.close();
				return;
			}
			do {
				members.add(new GuildCharacter(rs.getInt("id"), rs.getInt("level"), rs.getString("name"), (byte) -1, world, rs.getInt("job"), rs.getInt("guildrank"), guildid, false, rs.getInt("allianceRank")));
			} while (rs.next());
			setOnline(initiator.getId(), true, initiator.getChannel());
			ps.close();
			rs.close();
		} catch (SQLException se) {
			Output.print("Unable to read guild information from database.\n" + se);
			return;
		}
	}

	public void buildNotifications() {
		if (!bDirty) {
			return;
		}
		Set<Byte> chs = Server.getInstance().getChannelServer(world);
		if (notifications.keySet().size() != chs.size()) {
			notifications.clear();
			for (Byte ch : chs) {
				notifications.put(ch, new LinkedList<Integer>());
			}
		} else {
			for (List<Integer> l : notifications.values()) {
				l.clear();
			}
		}
		synchronized (members) {
			for (GuildCharacter mgc : members) {
				if (!mgc.isOnline()) {
					continue;
				}
				List<Integer> ch = notifications.get(mgc.getChannel());
				if (ch != null)
					ch.add(mgc.getId());
				// Unable to connect to Channel... error was here
			}
		}
		bDirty = false;
	}

	public void writeToDB(boolean bDisband) {
		try {
			Connection con = DatabaseConnection.getConnection();
			if (!bDisband) {
				StringBuilder builder = new StringBuilder();
				builder.append("UPDATE `guilds` SET `GP` = ?, `logo` = ?, `logoColor` = ?, `logoBG` = ?, `logoBGColor` = ?, ");
				for (int i = 0; i < 5; i++) {
					builder.append("rank").append(i + 1).append("title = ?, ");
				}
				builder.append("`capacity` = ?, `notice` = ? WHERE `guildid` = ?");
				PreparedStatement ps = con.prepareStatement(builder.toString());
				ps.setInt(1, gp);
				ps.setInt(2, logo);
				ps.setInt(3, logoColor);
				ps.setInt(4, logoBG);
				ps.setInt(5, logoBGColor);
				for (int i = 6; i < 11; i++) {
					ps.setString(i, rankTitles[i - 6]);
				}
				ps.setInt(11, capacity);
				ps.setString(12, notice);
				ps.setInt(13, this.id);
				ps.execute();
				ps.close();
			} else {
				PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET `guildid` = 0, `guildrank` = 5 WHERE `guildid` = ?");
				ps.setInt(1, this.id);
				ps.execute();
				ps.close();
				ps = con.prepareStatement("DELETE FROM `guilds` WHERE `guildid` = ?");
				ps.setInt(1, this.id);
				ps.execute();
				ps.close();
				this.broadcast(PacketCreator.guildDisband(this.id));
			}
		} catch (SQLException se) {
		}
	}

	public int getId() {
		return id;
	}

	public int getLeaderId() {
		return leader;
	}

	public int getGP() {
		return gp;
	}

	public int getLogo() {
		return logo;
	}

	public void setLogo(int l) {
		logo = l;
	}

	public int getLogoColor() {
		return logoColor;
	}

	public void setLogoColor(int c) {
		logoColor = c;
	}

	public int getLogoBG() {
		return logoBG;
	}

	public void setLogoBG(int bg) {
		logoBG = bg;
	}

	public int getLogoBGColor() {
		return logoBGColor;
	}

	public void setLogoBGColor(int c) {
		logoBGColor = c;
	}

	public String getNotice() {
		if (notice == null) {
			return "";
		}
		return notice;
	}

	public String getName() {
		return name;
	}

	public java.util.Collection<GuildCharacter> getMembers() {
		return java.util.Collections.unmodifiableCollection(members);
	}

	public int getCapacity() {
		return capacity;
	}

	public int getSignature() {
		return signature;
	}

	public void broadcast(GamePacket packet) {
		broadcast(packet, -1, BCOp.NONE);
	}

	public void broadcast(GamePacket packet, int exception) {
		broadcast(packet, exception, BCOp.NONE);
	}

	public void broadcast(GamePacket packet, int exceptionId, BCOp bcop) {
		synchronized (notifications) {
			if (bDirty) {
				buildNotifications();
			}
			try {
				for (Byte b : Server.getInstance().getChannelServer(world)) {
					if (notifications.get(b).size() > 0) {
						if (bcop == BCOp.DISBAND) {
							Server.getInstance().getWorld(world).setGuildAndRank(notifications.get(b), 0, 5, exceptionId);
						} else if (bcop == BCOp.EMBELMCHANGE) {
							Server.getInstance().getWorld(world).changeEmblem(this.id, notifications.get(b), new GuildSummary(this));
						} else {
							Server.getInstance().getWorld(world).sendPacket(notifications.get(b), packet, exceptionId);
						}
					}
				}
			} catch (Exception re) {
				Output.print("Failed to contact channels for broadcast."); // fu?
			}
		}
	}

	public void guildMessage(GamePacket serverNotice) {
		for (GuildCharacter mgc : members) {
			for (Channel cs : Server.getInstance().getChannelsFromWorld(world)) {
				if (cs.getPlayerStorage().getCharacterById(mgc.getId()) != null) {
					cs.getPlayerStorage().getCharacterById(mgc.getId()).getClient().getSession().write(serverNotice);
					break;
				}
			}
		}
	}

	public final void setOnline(int cid, boolean online, byte channel) {
		boolean bBroadcast = true;
		for (GuildCharacter mgc : members) {
			if (mgc.getId() == cid) {
				if (mgc.isOnline() && online) {
					bBroadcast = false;
				}
				mgc.setOnline(online);
				mgc.setChannel(channel);
				break;
			}
		}
		if (bBroadcast) {
			this.broadcast(PacketCreator.guildMemberOnline(id, cid, online), cid);
		}
		bDirty = true;
	}

	public void guildChat(String name, int cid, String msg) {
		this.broadcast(PacketCreator.multiChat(name, msg, 2), cid);
	}

	public String getRankTitle(int rank) {
		return rankTitles[rank - 1];
	}

	public static int createGuild(int leaderId, String name) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT `guildid` FROM `guilds` WHERE `name` = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (rs.first()) {
				ps.close();
				rs.close();
				return 0;
			}
			ps.close();
			rs.close();
			ps = con.prepareStatement("INSERT INTO `guilds` (`leader`, `name`, `signature`) VALUES (?, ?, ?)");
			ps.setInt(1, leaderId);
			ps.setString(2, name);
			ps.setInt(3, (int) System.currentTimeMillis());
			ps.execute();
			ps.close();
			ps = con.prepareStatement("SELECT `guildid` FROM `guilds` WHERE `leader` = ?");
			ps.setInt(1, leaderId);
			rs = ps.executeQuery();
			rs.first();
			int guildid = rs.getInt("guildid");
			rs.close();
			ps.close();
			return guildid;
		} catch (Exception e) {
			return 0;
		}
	}

	public int addGuildMember(GuildCharacter mgc) {
		synchronized (members) {
			if (members.size() >= capacity) {
				return 0;
			}
			for (int i = members.size() - 1; i >= 0; i--) {
				if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(mgc.getName()) < 0) {
					members.add(i + 1, mgc);
					bDirty = true;
					break;
				}
			}
		}
		this.broadcast(PacketCreator.newGuildMember(mgc));
		return 1;
	}

	public void leaveGuild(GuildCharacter mgc) {
		this.broadcast(PacketCreator.memberLeft(mgc, false));
		synchronized (members) {
			members.remove(mgc);
			bDirty = true;
		}
	}

	public void expelMember(GuildCharacter initiator, String name, int cid) {
		synchronized (members) {
			java.util.Iterator<GuildCharacter> itr = members.iterator();
			GuildCharacter mgc;
			while (itr.hasNext()) {
				mgc = itr.next();
				if (mgc.getId() == cid && initiator.getGuildRank() < mgc.getGuildRank()) {
					this.broadcast(PacketCreator.memberLeft(mgc, true));
					itr.remove();
					bDirty = true;
					try {
						if (mgc.isOnline()) {
							Server.getInstance().getWorld(mgc.getWorld()).setGuildAndRank(cid, 0, 5);
						} else {
							try {
								PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO `notes` (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
								ps.setString(1, mgc.getName());
								ps.setString(2, initiator.getName());
								ps.setString(3, "You have been expelled from the guild.");
								ps.setLong(4, System.currentTimeMillis());
								ps.executeUpdate();
								ps.close();
							} catch (SQLException e) {
								Output.print("Failed to expel a member from a guild.\n" + e);
							}
							Server.getInstance().getWorld(mgc.getWorld()).setOfflineGuildStatus((short) 0, (byte) 5, cid);
						}
					} catch (Exception re) {
						re.printStackTrace();
						return;
					}
					return;
				}
			}
			Output.print("Unable to find guild member with name " + name + " and id " + cid);
		}
	}

	public void changeRank(int cid, int newRank) {
		for (GuildCharacter mgc : members) {
			if (cid == mgc.getId()) {
				try {
					if (mgc.isOnline()) {
						Server.getInstance().getWorld(mgc.getWorld()).setGuildAndRank(cid, this.id, newRank);
					} else {
						Server.getInstance().getWorld(mgc.getWorld()).setOfflineGuildStatus((short) this.id, (byte) newRank, cid);
					}
				} catch (Exception re) {
					re.printStackTrace();
					return;
				}
				mgc.setGuildRank(newRank);
				this.broadcast(PacketCreator.changeRank(mgc));
				return;
			}
		}
	}

	public void setGuildNotice(String notice) {
		this.notice = notice;
		writeToDB(false);
		this.broadcast(PacketCreator.guildNotice(this.id, notice));
	}

	public void memberLevelJobUpdate(GuildCharacter mgc) {
		for (GuildCharacter member : members) {
			if (mgc.equals(member)) {
				member.setJobId(mgc.getJobId());
				member.setLevel(mgc.getLevel());
				this.broadcast(PacketCreator.guildMemberLevelJobUpdate(mgc));
				break;
			}
		}
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof GuildCharacter)) {
			return false;
		}
		GuildCharacter o = (GuildCharacter) other;
		return (o.getId() == id && o.getName().equals(name));
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
		hash = 89 * hash + this.id;
		return hash;
	}

	public void changeRankTitle(String[] ranks) {
		for (int i = 0; i < 5; i++) {
			rankTitles[i] = ranks[i];
		}
		this.broadcast(PacketCreator.rankTitleChange(this.id, ranks));
		this.writeToDB(false);
	}

	public void disbandGuild() {
		this.writeToDB(true);
		this.broadcast(null, -1, BCOp.DISBAND);
	}

	public void setGuildEmblem(short bg, byte bgcolor, short logo, byte logocolor) {
		this.logoBG = bg;
		this.logoBGColor = bgcolor;
		this.logo = logo;
		this.logoColor = logocolor;
		this.writeToDB(false);
		this.broadcast(null, -1, BCOp.EMBELMCHANGE);
	}

	public GuildCharacter getMGC(int cid) {
		for (GuildCharacter mgc : members) {
			if (mgc.getId() == cid) {
				return mgc;
			}
		}
		return null;
	}

	public boolean increaseCapacity() {
		if (capacity > 99) {
			return false;
		}
		capacity += 5;
		this.writeToDB(false);
		this.broadcast(PacketCreator.guildCapacityChange(this.id, this.capacity));
		return true;
	}

	public void gainGP(int amount) {
		this.gp += amount;
		this.writeToDB(false);
		this.guildMessage(PacketCreator.updateGP(this.id, this.gp));
	}

	public static GuildInviteResponse sendInvite(GameClient c, String targetName) {
		GameCharacter character = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
		if (character == null) {
			return GuildInviteResponse.NOT_IN_CHANNEL;
		}
		if (character.getGuildId() > 0) {
			return GuildInviteResponse.ALREADY_IN_GUILD;
		}
		character.getClient().getSession().write(PacketCreator.guildInvite(c.getPlayer().getGuildId(), c.getPlayer().getName()));
		return null;
	}

	public static void displayGuildRanks(GameClient c, int npcid) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `name`, `GP`, `logoBG`, `logoBGColor`, " + "`logo`, `logoColor` FROM `guilds` ORDER BY `GP` DESC LIMIT 50");
			ResultSet rs = ps.executeQuery();
			c.getSession().write(PacketCreator.showGuildRanks(npcid, rs));
			ps.close();
			rs.close();
		} catch (SQLException e) {
			Output.print("Failed to display guild ranks.\n" + e);
		}
	}

	public int getAllianceId() {
		return allianceId;
	}

	public void setAllianceId(int aid) {
		this.allianceId = aid;
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE `guilds` SET `allianceId` = ? WHERE `guildid` = ?");
			ps.setInt(1, aid);
			ps.setInt(2, id);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
		}
	}

	public int getIncreaseGuildCost(int size) {
		return 500000 * (size - 6) / 6;
	}
}