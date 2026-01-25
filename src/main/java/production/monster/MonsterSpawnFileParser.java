package production.monster;

public final class MonsterSpawnFileParser {
    public static MonsterSpawn FromLine(String line) {
        String[] parts = line.split("\t");

        if (parts.length < 4) {
            return null;
        }

        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            String name = parts[2];
            int respawnTicks = Integer.parseInt(parts[3]);

            MonsterDef def = MonsterRegistry.get(name);
            if (def == null) {
                System.err.println("Unknown monster: " + name);
                return null;
            }

            return new MonsterSpawn(x, y, def, respawnTicks);

        } catch (NumberFormatException e) {
            return null;
        }
    }
}
