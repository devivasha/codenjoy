package com.codenjoy.dojo.battlecity.model;

/*-
 * #%L
 * Codenjoy - it's a dojo-like platform from developers to developers.
 * %%
 * Copyright (C) 2018 Codenjoy
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.codenjoy.dojo.battlecity.model.levels.DefaultBorders;
import com.codenjoy.dojo.battlecity.services.Events;
import com.codenjoy.dojo.services.*;
import com.codenjoy.dojo.services.printer.BoardReader;

import java.util.LinkedList;
import java.util.List;

public class Battlecity implements Field {

    private Dice dice;
    private LinkedList<Tank> aiTanks;
    private int aiCount;

    private int size;
    private List<Construction> constructions;
    private List<Border> borders;

    private List<Player> players = new LinkedList<Player>();
    private final List<Bullet> bullets = new LinkedList<>();

    public Battlecity(int size, Dice dice, List<Construction> constructions, Tank... aiTanks) {
        this(size, dice, constructions, new DefaultBorders(size).get(), aiTanks);
    }

    public Battlecity(int size, Dice dice, List<Construction> constructions,
                      List<Border> borders, Tank... aiTanks) {
        aiCount = 6;
        this.dice = dice;
        this.size = size;
        this.aiTanks = new LinkedList<>();
        this.constructions = new LinkedList<>(constructions);
        this.borders = new LinkedList<>(borders);

        for (Tank tank : aiTanks) {
            addAI(tank);
        }
        generateBullets();
    }

    @Override
    public void clearScore() {
        players.forEach(Player::reset);
        constructions.forEach(Construction::reset);
        getTanks().forEach(Tank::reset);
    }

    @Override
    public void tick() {
        removeDeadTanks();

        newAI();

        List<Tank> tanks = getTanks();

        for (Tank tank : tanks) {
            tank.tick();
        }

//        for (Bullet bullet : getBullets()) {
//            if (bullet.destroyed()) {
//                bullet.onDestroy();
//            }
//        }

//        for (Tank tank : tanks) {
//            if (tank.isAlive()) {
//                tank.fire();
//            }
//        }

        for (int i = 0; i < tanks.size(); i++) {
            if (tanks.get(i).isAlive()) {
                tanks.get(i).move();
                for (int j = 0; j != i  && j < tanks.size(); j++) {
                    if (tanks.get(j).isAlive()) {
                        if (!(tanks.get(i).isGhost() && tanks.get(j).isGhost())) {
                            if (tanks.get(i).getPosition().itsMe(tanks.get(j).getPosition())) {
                                if (tanks.get(i).getDirection() == tanks.get(j).getDirection().inverted()) {
                                    tanks.get(i).kill(null);
                                    tanks.get(j).kill(null);
                                    break;
                                } else {
                                    if (tanks.get(i).getPosition().itsMe(tanks.get(i).getPreviousPosition())) {
                                        tanks.get(i).kill(null);
                                        break;
                                    } else {
                                        tanks.get(j).kill(null);
                                    }
                                }
                            }
//                        } else if ((tanks.get(i).getDirection() == tanks.get(j).getDirection().inverted()) &&
//                                isSame(tanks.get(i).getDirection(), tanks.get(i).getPosition(), tanks.get(j).getPosition())) {
//                            tanks.get(i).kill(null);
//                            tanks.get(j).kill(null);
//                        } else if ((tanks.get(i).getDirection() == tanks.get(j).getDirection()) &&
//                                isSame(tanks.get(i).getDirection(), tanks.get(i).getPosition(), tanks.get(j).getPosition())) {
//                            tanks.get(i).kill(null);
//                            tanks.get(j).kill(null);
//                        }
                        }
                    }
                }
            }
        }

//        for (int i = 0; i < tanks.size(); i++) {
//            if (tanks.get(i).isAlive()) {
//                tanks.get(i).move();
//            }
//        }

//        for (Tank tank : tanks) {
//            if (tank.isAlive()) {
//                tank.move();
//
//                List<Bullet> bullets = getBullets();
//                int index = bullets.indexOf(tank);
//                if (index != -1) {
//                    Bullet bullet = bullets.get(index);
//                    affect(bullet);
//                }
//            }
//        }
//        for (Bullet bullet : getBullets()) {
//            bullet.move();
//        }

        for (Construction construction : constructions) {
            if (!tanks.contains(construction) && !getBullets().contains(construction)) {
                construction.tick();
            }
        }
    }

    private boolean isSame(Direction d, Point p1, Point p2) {
        Point p = new PointImpl(p1);
        switch (d) {
            case UP: p.setY(p1.getY() + 1); break;
            case DOWN: p.setY(p1.getY() - 1); break;
            case RIGHT: p.setY(p1.getX() + 1); break;
            case LEFT: p.setY(p1.getX() - 1); break;
        }
        return p.itsMe(p2);
    }

    private void newAI() {
        for (int count = aiTanks.size(); count < aiCount; count++) {
            int y = size - 2;
            int x;
            int c = 0;
            do {
                x = dice.next(size);
            } while (isBarrier(x, y) && c++ < size);

            if (!isBarrier(x, y)) {
                addAI(new AITank(x, y, dice, Direction.DOWN));
            }
        }
    }

    private void generateBullets() {
        generateBullet();
    }

    private void generateBullet() {
        int x, y;

        for (int i = 0; i < 25; i++) {
                x = dice.next(this.size());
                y = dice.next(this.size());
            bullets.add(new Bullet(this, null, new PointImpl(x, y), null, null));
        }
    }

    private void removeDeadTanks() {
        for (Tank tank : getTanks()) {
            if (!tank.isAlive()) {
                aiTanks.remove(tank);
            }
        }
        for (Player player : players.toArray(new Player[0])) {
            if (!player.getHero().isAlive()) {
                players.remove(player);
            }
        }
    }
    void addAI(Tank tank) {
        tank.init(this);
        aiTanks.add(tank);
    }

    @Override
    public void affect(Bullet bullet) {
        if (borders.contains(bullet)) {
            bullet.onDestroy();
            return;
        }

        if (getTanks().contains(bullet)) {
            int index = getTanks().indexOf(bullet);
            Tank tank = getTanks().get(index);
//            if (tank == bullet.getOwner()) {
//                return;
//            }

            scoresForKill(bullet, tank);

//            tank.kill(bullet);
            bullet.onDestroy();  // TODO заимплементить взрыв
            bullets.remove(bullet);
            if(bullets.size() < 20)
                generateBullet();
            return;
        }

        for (Bullet bullet2 : getBullets().toArray(new Bullet[0])) {
            if (bullet != bullet2 && bullet.equals(bullet2)) {
                bullet.boom();
                bullet2.boom();
                return;
            }
        }

        if (constructions.contains(bullet)) {
            Construction construction = getConstructionAt(bullet);

            if (!construction.destroyed()) {
                construction.destroyFrom(bullet.getDirection());
                bullet.onDestroy();  // TODO заимплементить взрыв
            }

            return;
        }
    }

    private Construction getConstructionAt(Bullet bullet) {
        int index = constructions.indexOf(bullet);
        return constructions.get(index);
    }

    private void scoresForKill(Bullet killedBullet, Tank diedTank) {
        Player died = null;
//        boolean aiDied = aiTanks.contains(diedTank);
//        if (!aiDied) {
       try {
           died = getPlayer(diedTank);
       } catch (Exception e){

       }

//        }


//        Tank killerTank = killedBullet.getOwner();
//        Player killer = null;
//        if (!aiTanks.contains(killerTank)) {
//            killer = getPlayer(killerTank);
//        }
//
//        if (killer != null) {
//            if (aiDied) {
//                killer.event(Events.KILL_OTHER_AI_TANK);
//            } else {
//                killer.killHero();
//                killer.event(Events.KILL_OTHER_HERO_TANK.apply(killer.score()));
//            }
//        }
        if (died != null) {
//            died.event(Events.KILL_YOUR_TANK);
            died.event(Events.KILL_OTHER_HERO_TANK.apply(1));
        }
    }

    private Player getPlayer(Tank tank) {
        for (Player player : players) {
            if (player.getHero().equals(tank)) {
                return player;
            }
        }

        throw new RuntimeException("Танк игрока не найден!");
    }

    @Override
    public boolean isBarrier(int x, int y) {
        for (Construction construction : constructions) {
            if (construction.itsMe(x, y) && !construction.destroyed()) {
                return true;
            }
        }
        for (Point border : borders) {
            if (border.itsMe(x, y)) {
                return true;
            }
        }
//        for (Tank tank : getTanks()) {   //  TODO проверить как один танк не может проходить мимо другого танка игрока (не AI)
//            if (tank.itsMe(x, y)) {
//                return true;
//            }
//        }
        return outOfField(x, y);
    }

    @Override
    public boolean outOfField(int x, int y) { // TODO заменить все есть в point
        return x < 0 || y < 0 || y > size - 1 || x > size - 1;
    }

    private List<Bullet> getBullets() {
        //        for (Tank tank : getTanks()) {
//            for (Bullet bullet : tank.getBullets()) {
//                result.add(bullet);
//            }
//        }
        return bullets;
    }

    @Override
    public List<Tank> getTanks() {
        LinkedList<Tank> result = new LinkedList<>(aiTanks);
        for (Player player : players) {
//            if (player.getTank().isAlive()) { // TODO разремарить с тестом
            result.add(player.getHero());
//            }
        }
        return result;
    }

    @Override
    public void remove(Player player) {   // TODO test me
        players.remove(player);
    }

    @Override
    public void newGame(Player player) {
        if (!players.contains(player)) {
            players.add(player);
        }
        player.newHero(this);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public BoardReader reader() {
        return new BoardReader() {
            private int size = Battlecity.this.size;

            @Override
            public int size() {
                return size;
            }

            @Override
            public Iterable<? extends Point> elements() {
                return new LinkedList<Point>() {{
                    addAll(Battlecity.this.getBorders());
                    addAll(Battlecity.this.getTanks());
                    addAll(Battlecity.this.getConstructions());
                    addAll(Battlecity.this.getBullets());
                }};
            }
        };
    }

    @Override
    public List<Construction> getConstructions() {
        List<Construction> result = new LinkedList<>();
        for (Construction construction : constructions) {
            if (!construction.destroyed()) {
                result.add(construction);
            }
        }
        return result;
    }

    @Override
    public List<Border> getBorders() {
        return borders;
    }

    public void setDice(Dice dice) {
        this.dice = dice;
    }

}
