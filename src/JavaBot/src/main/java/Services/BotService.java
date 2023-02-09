package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

import com.ctc.wstx.shaded.msv_core.scanner.dtd.MessageCatalog;

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

        double safeRadiusPlayer = 200;
        double attackRadius = 300;
        double safeRadiusGasCloud = 20;
        GameObject NearestPlayer = findNearestPlayer(bot.getPosition());
        int state = setState(safeRadiusPlayer, NearestPlayer, attackRadius);
        String messageBot = "State :" + state;
        int effectActive = bot.getEffect();
        int tick = 0;

        if(gameState.getWorld().getCurrentTick() != null){
            tick = gameState.getWorld().getCurrentTick();
        }

        messageBot += (" Tick :" + tick);

        // DEFAULT ACTION
        // aksi yang otomatis
        playerAction.action = PlayerActions.Forward;
        //playerAction.heading = new Random().nextInt(360);
        if(effectActive == 1 || effectActive == 3 || effectActive == 5){ 
            playerAction.action = PlayerActions.StopAfterBurner;
            messageBot += " Action :Stop AfterBurner";
        }

        //state = 0;
        switch(state){
            case 1: // OFFENSIVE STATE
            
            playerAction.heading = getHeadingBetween(NearestPlayer);

            int distance = (int) getDistanceBetween(bot, NearestPlayer) - bot.getSize() - NearestPlayer.getSize();

            // if((bot.getSize() - distance / (bot.getSpeed())^2) > NearestPlayer.getSize()){ 
            //     // AFTER BURNER
            //     // akan menyala ketika size bot setelah memakai afterburner 
            //     // dan sampai di bot lawan lebih besar dari bot lawan.
            //     playerAction.action = PlayerActions.StartAfterBurner;
            //     messageBot += " Action :Start AfterBurner";
            // }
            
                if(effectActive == 1 || effectActive == 3 || effectActive == 5){ 
                    playerAction.action = PlayerActions.StopAfterBurner;
                    messageBot += " Action :Stop AfterBurner";
                }

                if(NearestPlayer.getSize() < bot.getSize()-40 && bot.getTeleportCount() > 0){
                    playerAction.action = PlayerActions.FireTeleport;
                    messageBot += " Action :Fire Teleport";
                }
                else {
                    playerAction.action = PlayerActions.FireTorpedoes;
                    messageBot += " Action :Fire Torpedoes";
                }
            
            
            // KAYANYA INI BUAT DI DEFENSIVE AJA
            // if(bot.getSize() < NearestPlayer.getSize()){
            //     // FIRE TORPEDO
            //     // bot menembakan torpedo ketika target lawan lebih besar dari bot
            //     // tujuannya untuk mereduksi ukuran bot lawan agar bisa dimakan.

            //     playerAction.action = PlayerActions.FireTorpedoes;
            //     messageBot += " Action :Fire Torpedoes";
            // }

            // if(!((bot.getSize() - distance / (bot.getSpeed())^2) > NearestPlayer.getSize())){
            //     if(){
                    
            //     }
            // }

            break;

            case 2: // DEFENSIVE STATE
            break;
        
            default: // GROW STATE

            //setHeadingToNearest(ObjectTypes.Food); // dia bakal langsung ngarah ke food terdekat

            GameObject nearestfood = findNearestObject(ObjectTypes.Food);
            GameObject nearestsuperfood = findNearestObject(ObjectTypes.SuperFood);
            GameObject nearestsupernova = findNearestObject(ObjectTypes.SupernovaPickup);
            if(getDistanceBetween(bot, nearestsupernova) <= getDistanceBetween(bot, nearestsuperfood)){
                if(isGasCloudNear(safeRadiusGasCloud)==0){
                    setHeadingToNearest(ObjectTypes.SupernovaPickup);
                }
                else{
                    playerAction.heading = rotate180(playerAction.heading);
                }
            }
            else{
                if(getDistanceBetween(bot, nearestsuperfood) < getDistanceBetween(bot, nearestfood)){
                    if(isGasCloudNear(safeRadiusGasCloud)==0){
                        setHeadingToNearest(ObjectTypes.SuperFood);
                    }
                    else{
                        playerAction.heading = rotate180(playerAction.heading);
                    }
                }
                else{
                    if(isGasCloudNear(safeRadiusGasCloud)==0){
                        setHeadingToNearest(ObjectTypes.Food);
                    }
                    else{
                        playerAction.heading = rotate180(playerAction.heading);
                    }
                }
            }
        }

        
        // PRIMARY ACTION
        // aksi yang paling krusial jika situasi saat ini memenuhi syaratnya
        calculateTeleport(NearestPlayer);

        
        System.out.println(messageBot);
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
                Distance = getDistanceBetween(bot, Player) - bot.getSize() - Player.getSize(); // dikurangi size
                if(Distance < minDistance && bot != Player){
                    minDistance = Distance;
                    NearestPlayer = Player;
                }
            }
        }

        return NearestPlayer;
    }

    private GameObject findNearestObject(ObjectTypes target){
    // Menghasilkan GameObject yang adalah Object yang paling dekat dengan bot agario
        
        GameObject NearestObject = bot;
            
        if(!gameState.getGameObjects().isEmpty()){
            List<GameObject> AllObject = gameState.getGameObjects();
            double minDistance = 999999999;
            double Distance;
            NearestObject = AllObject.get(0);

            for (GameObject Object : AllObject){
                if(Object.getGameObjectType() == target){
                    Distance = getDistanceBetween(bot, Object) - bot.getSize() - Object.getSize();
                    if(Distance < minDistance){
                        minDistance = Distance;
                        NearestObject = Object;
                    }
                }
                
            }
        }

        return NearestObject;
    }


    private int setState(double safeRadiusPlayer, GameObject NearestPlayer, double attackRadius){
    /*  Menghasilkan state dari bot untuk memilih algoritma greedy yang dipakai.
        0. Grow 
        1. Offensive = Nearest Player terdekat lebih kecil ukurannya
        2. Defensive = Nearest Player lebih besar dan ukurannya lebih besar
    */
        int state = 0;
        double Distance = getDistanceBetween(bot, NearestPlayer) - bot.getSize() - NearestPlayer.getSize();
        
        if(gameState.getWorld().getCurrentTick() != null){
            int tick = gameState.getWorld().getCurrentTick();
        }

        if(bot.getSize() > NearestPlayer.getSize() && Distance < attackRadius ||  bot.getSize() > 70){
            
            state = 1;
            
        }
        else {
            if(Distance <= safeRadiusPlayer){
                //state = 2;
            }
        }

        return state;
    }

    //cek kalo ada gas cloud di deket bot player kita
    private int isGasCloudNear(double safeRadiusGasCloud){
        GameObject gas = findNearestObject(ObjectTypes.GasCloud);

        if((getDistanceBetween(bot, gas) - bot.getSize() - gas.getSize()) <= safeRadiusGasCloud){
            return 1;
        }
        else{
            return 0;
        }
    }

    //ubah heading sebesar 180 derajat
    private int rotate180(int heading){
        int newheading = heading - 180;
        if(newheading < 0){
            newheading += 360;
        }
        return newheading;

    }

    //private void generalState();

    private void setHeadingToNearest(ObjectTypes target){
    /*  Merubah heading bot kearah GameObject terdekat
        sesuai tipe yang dimasukan dalam argumen.
        2. Food
        3. WormHole
        7. SuperFood
        8. SupernovaPickup
     */

        GameObject targetObject = findNearestObject(target);
        int heading = getHeadingBetween(targetObject);

        playerAction.heading = heading;
        
    }

    private void calculateTeleport(GameObject NearestPlayer){

        GameObject Teleporter = bot;
        Position botPos = bot.getPosition();
        Position telPos;
        int xDiff, yDiff;
        boolean found = false;
        double degree;

        // Find Out Teleport
        if(!gameState.getGameObjects().isEmpty()){
            List<GameObject> AllObject = gameState.getGameObjects();

            for(GameObject Object : AllObject){
                if(Object.getGameObjectType() == ObjectTypes.Teleporter){
                    telPos = Object.getPosition();
                    yDiff = Math.abs(telPos.getY() - botPos.getY());
                    xDiff = Math.abs(telPos.getX() - botPos.getX());
                    degree = Math.toDegrees(Math.atan2(yDiff, xDiff));
                    System.out.println("Degree Teleport: " + degree);
                    System.out.println("Heading: " + Object.currentHeading);

                    /* mencocokan heading teleproter dengan sudut dari garis lurus yang
                     * dibuat oleh posisi teleporter dan bot untuk mengetahui apakah 
                     * teleporter tersebut punya kita. Dengan variable eror.
                     */
                    if(Math.abs(degree - Object.currentHeading) < 90){
                        Teleporter = Object;
                        found = true;
                        break;
                    }
                }
            }
        }

        // teleport when its time
        if(found){
            if(getDistanceBetween(NearestPlayer, Teleporter) - NearestPlayer.getSize() < bot.getSize()){
                playerAction.action = PlayerActions.Teleport;
                System.out.println("TELEPORT");

            }
        }
    }
}
