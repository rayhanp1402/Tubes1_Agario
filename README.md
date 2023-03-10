# Tugas Besar 1 Strategi Algoritma IF 2211 Tahun 2022/2023
Implementasi Algoritma Greedy dalam Permainan "Galaxio"

## Daftar Isi
* [Algoritma Greedy](#strategi-greedy)
* [Struktur File](#struktur-file)
* [Requirements](#requirements)
* [Cara Kompilasi Program](#cara-kompilasi-program)
* [Cara Menjalankan Program](#cara-menjalankan-program)
* [Link Demo](#link-demo)
* [Authors](#authors)

## Algoritma Greedy 
Permainan Galaxio merupakan sejenis permainan bergenre *Battle Royale*, dimana para pemain akan berkompetisi
untuk dapat bertahan sampai akhir yang daerahnya telah ditentukan dan mengecil seiring berjalannya waktu.
Kami menggunakan sebuah strategi *Greedy* yang dibagi dalam tiga buah *state*, yaitu *state Grow*, 
*state Offensive*, dan *state Defensive*

### State Grow
*State Grow* merupakan *state* yang membuat Bot membesar dalam *size*-nya. Pada *state* ini,
bot akan mencari dan mengonsumsi objek-objek yang dapat dikonsumsi atau diambil, yaitu *food*, 
*superfood*, dan *supernova* dengan jarak terdekat. Dari ketiga objek tersebut, diberikan
prioritas dimana *supernova* memiliki prioritas tertinggi dan *food* memiliki prioritas terendah
untuk dikonsumsi/diambil oleh Bot. Pada *state* ini, Bot juga akan menghindari *Gas Cloud*. *State*
ini diberlakukan pada awal permainan, dimana kami definisikan awal permainan sebagai keadaan *World*
dari permainan dengan *tick* kurang dari 100.

### State Offensive
Setelah *tick* meraih 100, *state* akan berubah menjadi *state Offensive*, dimana bot berusaha untuk
tetap membesar, namun dengan menyerang Bot pemain lain untuk dikonsumsi. Syarat dari *state* ini dijalankan 
adalah ketika terdeteksi Bot lawan terdekat dengan *size* kurang dari *size* Bot kita. *State* ini juga
akan dijalankan apabila *size* dari Bot cukup besar, yaitu dengan batas *size* yang telah didefinisikan.
Bot mengimplementasikan aksi *torpedo salvo* untuk mengecilkan *size* dari Bot lawan terdekat untuk dikonsumsi,
aksi menembakkan *teleport* untuk menangkap Bot lawan, dan aksi *AfterBurner* untuk mengejar
Bot lawan.

### State Defensive
Apabila syarat-syarat untuk aktivasi *state Offensive* tidak terpenuhi, maka Bot akan memiliki *state Defensive*,
dimana pada *state* ini, Bot akan berusaha untuk melindungi diri ataupun melarikan diri dari Bot lawan yang berbahaya.
Implementasi aksi-aksinya adalah *AfterBurner* untuk melarikan diri dengan cepat dari Bot lawan, atau bahkan *teleport*
untuk melarikan diri dengan instan. Bot dapat melawan Bot lawan yang berbahaya dengan melakukan aksi *torpedo salvo*
kepada Bot lawan tersebut dengan jarak aman yang sudah didefinisikan. Selain melindungi diri dari ancaman Bot lawan,
Bot kita juga akan melindungi diri dengan mengaktifkan *shield* apabila terdapat proyektil *torpedo* dengan jarak
yang telah ditentukan dan melarikan diri dari proyektil *supernova*.

### Primary Action
*Primary action* merupakan aksi-aksi yang krusial/utama untuk dilakukan Bot dalam apapun *state*-nya. Aksi-aksi
ini meliputi menembakkan dan meledakkan *supernova*, mengaktifkan *teleport* yang telah ditembakkan, menghentikan
*AfterBurner* dengan kondisi tertentu, dan melakukan aksi sedemikian rupa sehingga Bot tidak keluar dari batas
*World* dari permainan. Adapun aksi *default* dimana Bot mencari dan mengkonsumsi *food* terdekat.


## Struktur File
```bash
.
???   README.md
???   Dockerfile
???   pom.xml
???
????????????doc
???       agario.pdf
???
????????????target
???       ????????????classes
???       ??? 
???       ????????????libs
???       ??? 
???       ????????????maven-archiver
???       ??? 
???       ????????????maven-status
???       ??? 
???       ????????????agario.jar
???
???
????????????src
        ????????????main
                ????????????java
                ???       ????????????Enums
                ???       ???
                ???       ????????????ObjectTypes.java
                ???       ???
                ???       ????????????PlayerActions.java
                ???
                ???
                ????????????Models
                ???       ????????????GameObject.java
                ???       ???
                ???       ????????????GameState.java
                ???       ???
                ???       ????????????GameStateDto.java
                ???       ???
                ???       ????????????PlayerAction.java
                ???       ???
                ???       ????????????Position.java
                ???       ???
                ???       ????????????World.java
                ???
                ???
                ????????????Services
                ???       ????????????BotService.java
                ???
                ???   
                ????????????Main.java
```

## Requirements
* Java Virtual Machine (JVM) versi 11 atau lebih baru.
* NodeJS
* .Net Core 3.1
* Apache Maven 3.8.4

## Cara Kompilasi Program
* Download file `starter-pack.zip` pada link [berikut](https://github.com/EntelectChallenge/2021-Galaxio/releases/tag/2021.3.2).
* Unzip file `starter-pack.zip` pada mesin eksekusi.
* Lakukan cloning repository ini sebagai folder ke dalam folder `starter-pack`.
* Kemudian, jalankan perintah built `mvn clean package` pada folder `Tubes1_agario` dengan terminal.
* Bila terdapat file `.jar` baru pada folder `target`, maka program berhasil dikompilasi.

## Cara Menjalankan Program
Untuk Windows, Anda dapat menggunakan cara berikut
### Pada Terminal secara Manual
* Masuk ke dalam folder `starter-pack`.
* Jalankan perintah `cd .\runner-publish\`, kemudian `dotnet GameRunner.dll`.
* Buat terminal baru dalam folder `starter-pack`.
* Jalankan perintah `cd .\engine-publish\`, kemudian `dotnet Engine.dll`.
* Buat terminal baru dalam folder `starter-pack`.
* Jalankan perintah `cd .\logger-publish\`, kemudian `dotnet Logger.dll`.
* Panggil sebanyak Bot yang diperbolehkan dalam permainan (dapat diubah dalam file `appsettings.json` pada folder `engine-publish` dan `folder-publish`).
Sebagai contoh, jalankan perintah `cd .\reference-bot-publish\`, kemudian `dotnet ReferenceBot.dll` untuk Bot Referensi dan
`java -jar .\Tubes1_Agario\src\JavaBot.jar` untuk Bot Agario (Bot yang kami implementasikan). File `JavaBot.jar` dapat di-rename.

### Dengan Batch File
```bash
@echo off
:: Game Runner
cd ./runner-publish/
start "" dotnet GameRunner.dll
:: Game Engine
cd ../engine-publish/
timeout /t 1
start "" dotnet Engine.dll
:: Game Logger
cd ../logger-publish/
timeout /t 1
start "" dotnet Logger.dll
:: Bots
cd ../reference-bot-publish/
timeout /t 3
start "" dotnet ReferenceBot.dll
timeout /t 3
start "" dotnet ReferenceBot.dll
timeout /t 3
start "" dotnet ReferenceBot.dll
timeout /t 3
start "" dotnet ReferenceBot.dll
cd ../
pause
```
Bagian :: Bots dapat dimodifikasi dari menjalankan Reference Bot menjadi Bot Agario.

## Visualiser
Visualiser dapat dijalankan setelah log aktifitas dari sebuah permainan tersimpan. Log tersebut disimpan pada
folder `logger-publish`. Silahkan unzip folder zip pada folder `visualiser` sesuai Operating System Anda.

## Link Demo
* [Tugas Besar 1_Strategi Algoritma_Agario](https://youtu.be/6z3QYSaY1G8)

## Authors
* [Naufal Syifa Firdaus - 13521050](https://github.com/nomsf)
* [Shidqi Indy Izhari - 13521097](https://github.com/shidqizh)
* [Rayhan Hanif Maulana Pradana - 13521112](https://github.com/rayhanp1402)