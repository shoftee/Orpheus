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
package scripting.map;

import client.GameClient;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class MapScriptManager {
	private static MapScriptManager instance = new MapScriptManager();
	private Map<String, MapScript> scripts = new HashMap<String, MapScript>();
	private ScriptEngineFactory sef;

	private MapScriptManager() {
		ScriptEngineManager sem = new ScriptEngineManager();
		sef = sem.getEngineByName("javascript").getFactory();
	}

	public static MapScriptManager getInstance() {
		return instance;
	}

	public boolean scriptExists(String scriptName, boolean firstUser) {
		File scriptFile = new File("scripts/map/" + (firstUser ? "onFirstUserEnter" : "onUserEnter") + "/" + scriptName + ".js");
		return scriptFile.exists();
	}

	public void getMapScript(GameClient c, String scriptName, boolean firstUser) {
		if (scripts.containsKey(scriptName)) {
			scripts.get(scriptName).start(new MapScriptMethods(c));
			return;
		}
		String type = "onUserEnter";
		if (firstUser) {
			type = "onFirstUserEnter";
		}
		File scriptFile = new File("scripts/map/" + type + "/" + scriptName + ".js");
		if (!scriptFile.exists()) {
			return;
		}
		FileReader fr = null;
		ScriptEngine portal = sef.getScriptEngine();
		try {
			fr = new FileReader(scriptFile);
			CompiledScript compiled = ((Compilable) portal).compile(fr);
			compiled.eval();
		} catch (ScriptException e) {
			System.err.println("THROW" + e);
		} catch (IOException e) {
			System.err.println("THROW" + e);
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
					System.err.println("ERROR CLOSING" + e);
				}
			}
		}
		MapScript script = ((Invocable) portal).getInterface(MapScript.class);
		scripts.put(scriptName, script);
		script.start(new MapScriptMethods(c));
	}
}
