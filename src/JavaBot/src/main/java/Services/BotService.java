package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;

    public BotService() {
        this.playerAction = new PlayerAction();
        this.gameState = new GameState();
    }


    public GameObject getBot() {
        return this.bot;
    }

    public void setBot(GameObject bot) {
        this.bot = bot;
    }

    public PlayerAction getPlayerAction() {
        return this.playerAction;
    }

    public void setPlayerAction(PlayerAction playerAction) {
        this.playerAction = playerAction;
    }

    public void computeNextPlayerAction(PlayerAction playerAction) {

        /* -------    KAMUS    ------- 
            Kamus Berisi variable lokal yang berguna untuk decision making 
            pergerakan bot.
        */

        double safeRadiusPlayer = 100;
        double attackRadius = 100;
        double safeRadiusGasCloud = 100;
        GameObject NearestPlayer = findNearestPlayer(bot.getPosition());
        int state = setState(safeRadiusPlayer, NearestPlayer, attackRadius);

        switch(state){
            case 1: // OFFENSIVE STATE
            break;

            case 2: // DEFENSIVE STATE
            break;
        
            default: // GROW STATE
        }

        //generalState();

        //System.out.println("Nearest Player: " + NearestPlayer.getId());

        playerAction.action = PlayerActions.Forward;
        playerAction.heading = new Random().nextInt(360);

        
        this.playerAction = playerAction;
        //System.out.println("Compute Finish \n");
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    private void updateSelfState() {
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    private int getHeadingBetween(GameObject otherObject) {
        // return
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

    private GameObject findNearestPlayer(Position botPosition){
    // Menghasilkan GameObject yang adalah Player yang paling dekat dengan bot agario

        GameObject NearestPlayer = bot;
        
        if(!gameState.getPlayerGameObjects().isEmpty()){
            //System.out.println("inside conditional");
            List<GameObject> AllPlayer = gameState.getPlayerGameObjects();
            double minDistance = 999999999;
            double Distance;
            NearestPlayer = AllPlayer.get(0);

            for (GameObject Player : AllPlayer){
                Distance = getDistanceBetween(bot, Player); 
                if(Distance < minDistance && bot != Player){
                    minDistance = Distance;
                    NearestPlayer = Player;
                }
            }
        }

        return NearestPlayer;
    }


    private int setState(double safeRadiusPlayer, GameObject NearestPlayer, double attackRadius){
    /*  Menghasilkan state dari bot untuk memilih algoritma greedy yang dipakai.
        0. Grow 
        1. Offensive = Nearest Player terdekat lebih kecil ukurannya
        2. Defensive = Nearest Player lebih besar dan ukurannya lebih besar
    */
        int state = 0;
        double Distance = getDistanceBetween(bot, NearestPlayer);

        if(bot.getSize() > NearestPlayer.getSize()){
            if(Distance <= attackRadius){
                state = 1;
            }  
        }
        else {
            if(Distance <= safeRadiusPlayer){
                state = 2;
            }
        }

        return state;
    }

    //private void generalState();


}
