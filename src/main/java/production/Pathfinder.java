package production;

import production.tilemap.Tile;
import production.tilemap.TileMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * BFS pathfinding on the tile map.
 * Coordinates are packed into ints: high 16 bits = x, low 16 bits = y.
 */
public final class Pathfinder {

    private Pathfinder() {}

    /**
     * Pack two tile coordinates into a single int.
     * Supports signed 16-bit range (-32768 to 32767).
     */
    public static int pack(int x, int y) {
        return ((x & 0xFFFF) << 16) | (y & 0xFFFF);
    }

    public static int unpackX(int packed) {
        return (short)(packed >> 16);
    }

    public static int unpackY(int packed) {
        return (short)(packed);
    }

    /**
     * Find a path from start to destination using BFS.
     *
     * @param map the tile map
     * @param startX starting tile X
     * @param startY starting tile Y
     * @param destX destination tile X
     * @param destY destination tile Y
     * @return path as ArrayList of packed coordinates (start excluded, dest included),
     *         or null if no path exists
     */
    public static ArrayList<Integer> find(TileMap map,
                                          int startX, int startY,
                                          int destX, int destY) {
        /* trivial case: already there */
        if (startX == destX && startY == destY) {
            return new ArrayList<Integer>();
        }

        /* check destination is valid and walkable */
        Tile destTile = map.getTile((short) destX, (short) destY);
        if (destTile == null || destTile.blocked) {
            return null;
        }

        int startPacked = pack(startX, startY);
        int destPacked = pack(destX, destY);

        /* BFS frontier */
        Queue<Integer> frontier = new LinkedList<Integer>();
        frontier.add(startPacked);

        /* track where we came from: cameFrom[node] = previous node */
        HashMap<Integer, Integer> cameFrom = new HashMap<Integer, Integer>();
        cameFrom.put(startPacked, null);

        /* direction offsets: right, left, down, up */
        int[] dx = { 1, -1, 0, 0 };
        int[] dy = { 0, 0, 1, -1 };

        while (!frontier.isEmpty()) {
            int current = frontier.poll();
            int cx = unpackX(current);
            int cy = unpackY(current);

            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];
                int neighborPacked = pack(nx, ny);

                /* already visited? */
                if (cameFrom.containsKey(neighborPacked)) continue;

                /* valid tile? */
                Tile neighborTile = map.getTile((short) nx, (short) ny);
                if (neighborTile == null || neighborTile.blocked) continue;

                cameFrom.put(neighborPacked, current);

                /* found destination? */
                if (neighborPacked == destPacked) {
                    return reconstructPath(cameFrom, destPacked);
                }

                frontier.add(neighborPacked);
            }
        }

        /* no path found */
        return null;
    }

    private static ArrayList<Integer> reconstructPath(
            HashMap<Integer, Integer> cameFrom, int dest) {
        ArrayList<Integer> path = new ArrayList<Integer>();
        Integer current = dest;

        while (current != null) {
            Integer prev = cameFrom.get(current);
            if (prev != null) {  /* don't include start */
                path.add(current);
            }
            current = prev;
        }

        /* reverse so path goes start -> dest */
        int size = path.size();
        for (int i = 0; i < size / 2; i++) {
            int temp = path.get(i);
            path.set(i, path.get(size - 1 - i));
            path.set(size - 1 - i, temp);
        }

        return path;
    }
}