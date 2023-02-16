package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;
import java.lang.Math;

import com.azure.core.annotation.HeaderCollection;
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

        
        double attackRadius = 300;
        double safeRadiusGasCloud = 20;
        double safeRadiusSupernova = 200;
        double safeEnemyTorpedoRadius = 150;
        GameObject NearestPlayer = findNearestPlayer(bot.getPosition());
        double safeRadiusPlayer =150;
        int state = setState(safeRadiusPlayer, NearestPlayer, attackRadius);
        String messageBot = "";
        int effectActive = bot.getEffect();
        int tick = 0;


        /* --------   ALGORITMA  -------- */

        if(gameState.getWorld().getCurrentTick() != null){
            tick = gameState.getWorld().getCurrentTick();
        }

        messageBot += (" Tick :" + tick);

        // DEFAULT ACTION
        // aksi yang otomatis
        playerAction.action = PlayerActions.Forward;
        setHeadingToNearest(ObjectTypes.Food);

        //playerAction.heading = new Random().nextInt(360);
        // if(effectActive == 1 || effectActive == 3 || effectActive == 5){ 
        //     playerAction.action = PlayerActions.StopAfterBurner;
        //     messageBot += " Action :Stop AfterBurner";
        // }

        messageBot += " State :" + state + " Effect: " + effectActive + " Size : " + bot.getSize();

        switch(state){
            case 1: // OFFENSIVE STATE
            
            playerAction.heading = getHeadingBetween(NearestPlayer);

            int distance = (int) getDistanceBetween(bot, NearestPlayer) - bot.getSize() - NearestPlayer.getSize();

            if((bot.getSize() - distance / (bot.getSpeed())^2) > NearestPlayer.getSize()){ 
                // AFTER BURNER
                // akan menyala ketika size bot setelah memakai afterburner 
                // dan sampai di bot lawan lebih besar dari bot lawan.
                playerAction.action = PlayerActions.StartAfterBurner;
                messageBot += " Action :Start AfterBurner";
            }
            if(NearestPlayer.getSpeed() < 15){
                fireTorpedo();
                messageBot += " Action :Fire Torpedoes";
            }
            if(NearestPlayer.getSize() < bot.getSize()-40){
                fireTeleport();
                messageBot += " Action :Fire Teleport";
            }
            break;

            case 2: // DEFENSIVE STATE
            GameObject nearestSupernovaBomb = findNearestObject(ObjectTypes.SupernovaBomb);
            int botHeading = getHeadingBetween(nearestSupernovaBomb) - 180;
            //playerAction.heading = NearestPlayer.currentHeading;

            shieldActivation(safeEnemyTorpedoRadius);

            if(bot.getSize() < NearestPlayer.getSize() && getDistanceBetween(bot, NearestPlayer) <= safeRadiusPlayer) {
                

                //playerAction.heading = NearestPlayer.currentHeading + 45;

                if(bot.getSize() > 5) {
                    // Kabur menggunakan afterburner apabila syarat memenuhi
                    playerAction.action = PlayerActions.StartAfterBurner;
                    messageBot += " Action: AfterBurner Run from Player";
                }

                // Menembakkan teleport untuk kabur jika syarat memenuhi
                fireTeleport(); // ini gabisa pake yang normal calculatenya

                if(bot.getSize() < NearestPlayer.getSize()) {
                    // bot menembakkan torpedo ketika target lawan lebih besar dari bot
                    // tujuannya untuk mereduksi ukuran bot lawan agar bisa dimakan.
                    playerAction.heading = getHeadingBetween(NearestPlayer);
                    fireTorpedo();
                    messageBot += " Action: Fire Torpedo";
                }

                    escapeFromPlayerHeading(playerAction.heading);
                    // Menembakkan teleport untuk kabur jika syarat memenuhi
                    fireTeleport();

                    if(bot.getSize() > 5) {
                        // Kabur menggunakan afterburner apabila syarat memenuhi
                        playerAction.action = PlayerActions.StartAfterBurner;
                        messageBot += " Action:AfterBurner Run from Player";
                    }

                    if(getDistanceBetween(bot, nearestSupernovaBomb) <= safeRadiusSupernova) {
                        playerAction.heading = nearestSupernovaBomb.currentHeading + 90;
                        // Menembakkan teleport untuk kabur jika syarat memenuhi
                        fireTeleport();
                    if(bot.getSize() > 5) {
                        // Kabur menggunakan afterburner apabila syarat memenuhi
                        playerAction.action = PlayerActions.StartAfterBurner;
                        messageBot += " Action:AfterBurner Run from Supernova";
                        }
                    }
                }
                else {
                    rotateNearGas(playerAction.heading);
                }
            

            escapeTeleport(safeRadiusPlayer);
            break;
        
            default: // GROW STATE

            GameObject nearestfood = findNearestObject(ObjectTypes.Food);
            GameObject nearestsuperfood = findNearestObject(ObjectTypes.SuperFood);
            GameObject nearestsupernova = findNearestObject(ObjectTypes.SupernovaPickup);
            if(getDistanceBetween(bot, nearestsupernova) <= getDistanceBetween(bot, nearestsuperfood) && nearestsupernova.getGameObjectType() == ObjectTypes.SupernovaPickup){
                if(isGasCloudNear(safeRadiusGasCloud)==0){
                    setHeadingToNearest(ObjectTypes.SupernovaPickup);
                    messageBot += " Action : Heading for picking a supernova";
                }
                else{
                    rotateNearGas(playerAction.heading);
                }
            }
            else{
                if(getDistanceBetween(bot, nearestsuperfood) < getDistanceBetween(bot, nearestfood)){
                    if(isGasCloudNear(safeRadiusGasCloud)==0){
                        setHeadingToNearest(ObjectTypes.SuperFood);
                        messageBot += " Action : Heading for picking a superfood";
                    }
                    else{
                        rotateNearGas(playerAction.heading);
                    }
                }
                else{
                    if(isGasCloudNear(safeRadiusGasCloud)==0){
                        setHeadingToNearest(ObjectTypes.Food);
                        messageBot += " Action : Heading for picking a regular food";
                    }
                    else{
                        rotateNearGas(playerAction.heading);
                    }
                }
            }

            if(effectActive == 1 || effectActive == 3 || effectActive == 5){ 
                playerAction.action = PlayerActions.StopAfterBurner;
                messageBot += " Action :Stop AfterBurner";
            }

        }
        
    

        
        // PRIMARY ACTION
        // aksi yang paling krusial jika situasi saat ini memenuhi syaratnya
        setANewHeadingIfOutOfBound(playerAction.heading);
        calculateTeleportAlternative(NearestPlayer);
        stopAfterBurner(NearestPlayer);
        fireOrDetonateSupernova();

        
        
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
                int objX = Player.getPosition().getX();
                int objY = Player.getPosition().getY();
                int objToCenter = (int) Math.sqrt(Math.pow(objX, 2) + Math.pow(objY, 2));
                
                if(Distance < minDistance && bot != Player && objToCenter < gameState.getWorld().getRadius()){
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
                    int objX = Object.getPosition().getX();
                    int objY = Object.getPosition().getY();
                    int objToCenter = (int) Math.sqrt(Math.pow(objX, 2) + Math.pow(objY, 2));

                    if(Distance < minDistance && objToCenter < gameState.getWorld().getRadius()){
                        minDistance = Distance;
                        NearestObject = Object;
                    }
                }
                
            }
        }

        return NearestObject;
    }
    
    private GameObject findNearestObjectBetween(GameObject source,ObjectTypes target){
        // Menghasilkan GameObject yang adalah Object yang paling dekat dengan gameobject course
        // berdasarkan tipe yang dimasukan
            
            GameObject NearestObject = source;
                
            if(!gameState.getGameObjects().isEmpty()){
                List<GameObject> AllObject = gameState.getGameObjects();
                double minDistance = 999999999;
                double Distance;
                NearestObject = AllObject.get(0);
    
                for (GameObject Object : AllObject){
                    if(Object.getGameObjectType() == target){
                        Distance = getDistanceBetween(source, Object) - source.getSize() - Object.getSize();
                        if(Distance < minDistance){
                            minDistance = Distance;
                            NearestObject = Object;
                        }
                    }
                    
                }
            }
    
            return NearestObject;
        }

    private GameObject findNearestSafeObject(ObjectTypes target){
        // Menghasilkan GameObject yang adalah Object yang paling dekat dengan bot agario
        // dengan syarat game object tersebut tidak dekat dengan objek berbahaya.
            
            GameObject NearestObject = bot;
            GameObject gas;
            GameObject asteroid;
                
            if(!gameState.getGameObjects().isEmpty()){
                List<GameObject> AllObject = gameState.getGameObjects();
                double minDistance = 999999999;
                double Distance;
                NearestObject = AllObject.get(0);
    
                for (GameObject Object : AllObject){
                    gas = findNearestObjectBetween(Object, ObjectTypes.GasCloud);
                    asteroid = findNearestObjectBetween(Object, ObjectTypes.AsteroidField);

                    if(Object.getGameObjectType() == target){
                        Distance = getDistanceBetween(bot, Object) - bot.getSize() - Object.getSize();
                        
                        if(Distance < minDistance 
                            && getDistanceBetween(Object, gas) + gas.getSize() < bot.getSize()
                            && getDistanceBetween(Object, asteroid) + asteroid.getSize() < bot.getSize()){
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
        

        if((bot.getSize() > NearestPlayer.getSize() && Distance < attackRadius) || bot.getSize() > 500){
            
            state = 1;
            
        }
        else {
            if(Distance <= safeRadiusPlayer){
                state = 2;
            }
        }

        if(gameState.getWorld().getCurrentTick() != null){
            if(gameState.getWorld().getCurrentTick() < 100){
                state = 0;
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

    //ubah heading jika ketemu dengan gascloud
    private void rotateNearGas(int heading){ // bug
        GameObject gas = findNearestObject(ObjectTypes.GasCloud);
        int x = bot.getPosition().getX();
        int y = bot.getPosition().getY();
        int x2 = gas.getPosition().getX();
        int y2 = gas.getPosition().getY();
        double deltaX = x-x2;
        double deltaY = y-y2;
        double newheading = Math.atan2(deltaY, deltaX);
        newheading += Math.PI;
        playerAction.heading = (int) newheading;
    }
    
    private void setANewHeadingIfOutOfBound(int heading){
        // mengubah heading jika ketemu dengan bound/radius luar game
        int newheading = heading;
        double distance = Math.sqrt(Math.pow(bot.getPosition().getX(), 2) + Math.pow(bot.getPosition().getY(), 2));
        GameObject centerWorld = bot;
        Position zero = bot.getPosition();

        zero.setX(0); zero.setY(0);
        centerWorld.setPosition(zero);
        
        if(gameState.getWorld().getRadius() != null){
            if((int) distance + bot.getSize() + 10> gameState.getWorld().getRadius()){
                // double sudut = Math.atan2(bot.getPosition().getX(),bot.getPosition().getY());
                // double nheading = sudut + Math.PI;
                // newheading = (int) nheading;
                newheading = getHeadingBetween(centerWorld);

                System.out.println("PRIMARY: On The Edge, Change Heading to: ");
                System.out.println(newheading);
            }
        }
        playerAction.heading = newheading;
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

    private void calculateTeleportAlternative(GameObject NearestPlayer){
        GameObject Teleporter = bot;

        if(!gameState.getGameObjects().isEmpty()){
            List<GameObject> AllObject = gameState.getGameObjects();

            for(GameObject Object : AllObject){
                if(Object.getGameObjectType() == ObjectTypes.Teleporter){
                    Teleporter = Object;
                    if(getDistanceBetween(Teleporter, NearestPlayer) < bot.getSize() && bot.getSize() > NearestPlayer.getSize()){
                        playerAction.action = PlayerActions.Teleport;
                        System.out.println("PRIMARY: Teleport ");
                    }
                }
            }
        }
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
                    degree = (int) Math.toDegrees(Math.atan2(yDiff, xDiff));
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

    private void fireTorpedo() {
    // periksa apakah ukuran bot cukup untuk tembak torpedo
        
        boolean otherTeleport = false; // gaada teleport lain

        if(!gameState.getGameObjects().isEmpty()){
            List<GameObject> AllObject = gameState.getGameObjects();

            for(GameObject Object : AllObject){
                if(Object.getGameObjectType() == ObjectTypes.Teleporter){
                    otherTeleport = true;
                }
            }
        }
    
    
        if(bot.getSize() > 50 && (!otherTeleport)){
        // tembak teleporter kalau size cukup dan gaada teleport lain yang udh ditembak atau dari musuh
            playerAction.action = PlayerActions.FireTorpedoes;
        }
    }

    private void stopAfterBurner(GameObject nearestPlayer){
    // langsung matikan afterburner jika ukuran kapal kritis

        if((bot.getEffect() == 1 || bot.getEffect() == 3 || bot.getEffect() == 5) && bot.getSize() <= nearestPlayer.getSize()){
            playerAction.action = PlayerActions.StopAfterBurner;
            System.out.println("PRIMARY: Stop Afterburner");
        }
    }

    private void fireTeleport(){
    // tembakan teleport jika syarat terpenuhi
        if(bot.getTeleportCount() > 0 && bot.getSize() > 60){
            playerAction.action = PlayerActions.FireTeleport;
        }
    }

    private GameObject findFurthestPlayer(){
        // Menghasilkan GameObject yang adalah Player yang paling dekat dengan bot agario
    
            GameObject furthestPlayer = bot;
            
            if(!gameState.getPlayerGameObjects().isEmpty()){
                //System.out.println("inside conditional");
                List<GameObject> AllPlayer = gameState.getPlayerGameObjects();
                double maxDistance = 0;
                double Distance;
                furthestPlayer = AllPlayer.get(0);
    
                for (GameObject Player : AllPlayer){
                    Distance = getDistanceBetween(bot, Player) - bot.getSize() - Player.getSize(); // dikurangi size
                    if(Distance < maxDistance && bot != Player){
                        maxDistance = Distance;
                        furthestPlayer = Player;
                    }
                }
            }
    
            return furthestPlayer;
        }

    
        private void fireOrDetonateSupernova(){

        boolean foundSupernova = false;
        
        if(!gameState.getPlayerGameObjects().isEmpty()){
            List<GameObject> AllObject = gameState.getPlayerGameObjects();
            for (GameObject Object : AllObject){
                if(Object.getGameObjectType() == ObjectTypes.SupernovaBomb){
                    foundSupernova = true;
                }
            }
        }

        if(foundSupernova){
            playerAction.action = PlayerActions.DetonateSupernova;
            System.out.println("PRIMARY: Detonate Supernova");
        }
        else if(bot.getSupernovaAvailable() != 0){
            GameObject furthestPlayer = findFurthestPlayer();

            playerAction.heading = getHeadingBetween(furthestPlayer);
            playerAction.action = PlayerActions.FireSupernova;
            System.out.println("PRIMARY: Fire Supernova");
        }


    }

    private void shieldActivation(double safeEnemyTorpedoRadius) {
        // Mengatkifkan shield apabila terdapat torpedo salvo dalam radius bahaya
        // Syarat : Shield count > 0 dan size player > 20
        GameObject nearestTorpedo = findNearestObject(ObjectTypes.TorpedoSalvo);
        if(getDistanceBetween(bot, nearestTorpedo) - bot.getSize() <= safeEnemyTorpedoRadius && bot.getShieldCount() > 0 && bot.getSize() > 50) {
            playerAction.action = PlayerActions.ActivateShield;
            System.out.println("Shield Activated");
        }
    }

    private void escapeTeleport(double safeRadiusPlayer){
        GameObject Teleporter = bot;

        if(!gameState.getGameObjects().isEmpty()){
            List<GameObject> AllObject = gameState.getGameObjects();

            for(GameObject Object : AllObject){
                if(Object.getGameObjectType() == ObjectTypes.Teleporter){
                    Teleporter = Object;
                    if(getDistanceBetween(Teleporter, bot) - bot.getSize() >= safeRadiusPlayer){
                        playerAction.action = PlayerActions.Teleport;
                        System.out.println("ESCAPE: Teleport ");
                    }
                }
            }
        }
    }

     private void escapeFromPlayerHeading(int heading) {
        GameObject NearestPlayer = findNearestPlayer(bot.getPosition());
        int x1 = NearestPlayer.getPosition().getX();
        int y1 = NearestPlayer.getPosition().getY();
        int x2 = bot.getPosition().getX();
        int y2 = bot.getPosition().getY();

        double escapeHeading = Math.atan2(x2 - x1, y2 - y1);
        escapeHeading += Math.PI/4;

        playerAction.heading = toDegrees(escapeHeading);
    }
}