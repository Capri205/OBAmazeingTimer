# OBAmazeingTimer
No, it's not an amazing timer. It's a rudimentary timer for a maze. In particular, for the awesome maze
on our build server called "A-Maze-Ing".

This plugin will automatically time a players attempt to solve A-Maze-Ing, and update a sign based
leaderboard with the player name, the time and their rank.

Triggers for the start/entry and stop/exit are customizable, along with the maze dimensions for
detecting when a player leaves the maze. The leaderboard is also configurable for placement
and materials, as well as the text color and direction the leaderboard faces.

The elapsed time during a run is displayed in the action bar, as well as the final time as a title
message when you exit the maze by triggering the exit/stop trigger.

Main configuration file contains the start and stop triggers and borders of the maze. The Maze borders
are defined as two corners (so a square maze) and a low and high Y coordinate value. A listener will
detect if you go out of the maze boundary and will cancel the timer, as will leaving the game, getting
killed or warping out etc.

The leaderboard config file contains players and their times. The file contains the times as a hash with
player uuid as the key and their time. This is so we can have one time for a player - uniquness of key.
However, the in-game hash used for the leaderboard is a TreeMap using the time as the key and player as
the value. This is because a TreeMap will automatically add new entries in numeric ascending order, which
is great the for leaderboard as no sorting required and the signs can be rendered directly from the hash.

The signs configuration file contains the sign direction, location, sign and backing materials, as well
as the color of the text on the signs. Title and leaderboard are set separately with a flag for whether
you want to draw the title or not. There is also a setting for the number of sign lines the leaderboard
contains. Each sign line on the leaderboard is a set of 3 horizontally oriented signs which show rank,
player name, and the time. Since a sign has 4 lines, a 3 row leaderboard will have 12 ranks, which is
also the default.

The timer of course can be used for things other than a maze.
Here is my gaudy looking leaderboard showing the title row and 3 leaderboard rows:

![OBAmazeingTimer-leaderboard](https://ob-mc.net/repo/OBAmazeingTimer-leaderboard.png)
![OBAmazeing](https://ob-mc.net/repo/OBAmazeing.png)

You can find A-Maze-Ing in our origial amusement park area at X=634, Z=-85. Join ob-mc.net!

Compiled for 1.21 with Java 21.