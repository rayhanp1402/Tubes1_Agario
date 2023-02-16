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

        double safeRadiusPlayer = 200;
        double attackRadius = 300;
        double safeRadiusGasCloud = 20;
        double safeRadiusSupernova = 200;
        double safeEnemyTorpedoRadius = 50;
        GameObject NearestPlayer = findNearestPlayer(bot.getPosition());
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
        //playerAction.heading = new Random().nextInt(360);
        if(effectActive == 1 || effectActive == 3 || effectActive == 5){ 
            playerAction.action = PlayerActions.StopAfterBurner;
            messageBot += " Action :Stop AfterBurner";
        }

        if(tick > 50){
            state = 1;
        }

        messageBot += " State :" + state;

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
            if(effectActive == 1 || effectActive == 3 || effectActive == 5){ 
                playerAction.action = PlayerActions.StopAfterBurner;
                messageBot += " Action :Stop AfterBurner";
            }

            GameObject nearestSupernovaBomb = findNearestObject(ObjectTypes.SupernovaBomb);

            shieldActivation(safeEnemyTorpedoRadius);

            if(bot.getSize() < NearestPlayer.getSize() && getDistanceBetween(bot, NearestPlayer) <= safeRadiusPlayer) {
                if(isGasCloudNear(safeRadiusGasCloud)==0) {
                    if(bot.getSize() > NearestPlayer.getSize() - 5) {
                    // bot menembakkan torpedo ketika target lawan lebih besar dari bot
                    // tujuannya untuk mereduksi ukuran bot lawan agar bisa dimakan.
                        playerAction.heading = getHeadingBetween(NearestPlayer);
                        fireTorpedo();
                        messageBot += " Action:Fight back";
                    }

                    playerAction.heading = NearestPlayer.currentHeading + 45;
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
                    playerAction.heading = rotate180(playerAction.heading);
                }
            }
            break;
        
            default: // GROW STATE

            if(effectActive == 1 || effectActive == 3 || effectActive == 5){ 
                playerAction.action = PlayerActions.StopAfterBurner;
                messageBot += " Action :Stop AfterBurner";
            }

            GameObject nearestfood = findNearestObject(ObjectTypes.Food);
            GameObject nearestsuperfood = findNearestObject(ObjectTypes.SuperFood);
            GameObject nearestsupernova = findNearestObject(ObjectTypes.SupernovaPickup);
            if(getDistanceBetween(bot, nearestsupernova) <= getDistanceBetween(bot, nearestsuperfood)){
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
        }

        
        // PRIMARY ACTION
        // aksi yang paling krusial jika situasi saat ini memenuhi syaratnya
        setANewHeadingIfOutOfBound(playerAction.heading);
        calculateTeleportAlternative(NearestPlayer);
        stopAfterBurner();
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
        if((int) distance > gameState.getWorld().getRadius()){
            double sudut = Math.atan2(bot.getPosition().getX(),bot.getPosition().getY());
            double nheading = sudut + Math.PI;
            newheading = (int) nheading;
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
                        System.out.println(" TELEPORT ");
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

    private void stopAfterBurner(){
    // langsung matikan afterburner jika ukuran kapal kritis

        if((bot.getEffect() == 1 || bot.getEffect() == 3 || bot.getEffect() == 5) && bot.getSize() <= 50 ){
            playerAction.action = PlayerActions.StopAfterBurner;
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
        }
        else if(bot.getSupernovaAvailable() != 0){
            GameObject furthestPlayer = findFurthestPlayer();

            playerAction.heading = getHeadingBetween(furthestPlayer);
            playerAction.action = PlayerActions.FireSupernova;
        }


    }

    private void shieldActivation(double safeEnemyTorpedoRadius) {
        // Mengatkifkan shield apabila terdapat torpedo salvo dalam radius bahaya
        // Syarat : Shield count > 0 dan size player > 20
        GameObject nearestTorpedo = findNearestObject(ObjectTypes.TorpedoSalvo);
        if(getDistanceBetween(bot, nearestTorpedo) <= safeEnemyTorpedoRadius && bot.getShieldCount() > 0 && bot.getSize() > 20) {
            playerAction.action = PlayerActions.ActivateShield;
            System.out.println("Shield Activated");
        }
    }
}
