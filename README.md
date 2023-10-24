[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-24ddc0f5d75046c5622901739e7c5dd533143b0c8e959d652212380cedb1ea36.svg)](https://classroom.github.com/a/fnZ3vxy8)

# TEAM BLOCKNET REPOSITORY
 ## 프로젝트
  ### 1. 프로젝트 명
  블록체인 기반 중고거래 플랫폼
  ### 2. 목적
  기존 온라인 중고거래의 문제들을 블록체인을 통한 솔루션으로 개선한 중고거래 플랫폼 개발
  ### 3. 요약 
  기존 온라인 중고거래 플랫폼에서는 온라인 전자결제의 비효율적인 구조로 높은 수수료를 지불하며 거래 하고 , 구매자와 판매자간 의 정보의 비대칭성으로 인한 신뢰문제로 사기거래가 발생하는 경우가 많아 피해를 보는 사례가 발생하고 있다. 많은 문제를 가진 기존 중고거래 플랫폼을 개선하기위해 "블록체인 기반 중고거래 플랫폼"을 개발하여 수수료를 절감하고, 블록체인의 무결성 특성을 바탕으로 정보의 대칭성을 보장 할 수 있도록 추가 기능들을 설계하여 신뢰할수 있고 경제적인 중고거래를 가능하게 하고자 한다. 스마트컨트랙을 통한 이더리움 기반 결제 방식을 도입해 결제 수수료를 대폭 줄였으며, 분산원장 기술을 사용하는 블록체인의 무결성을 기반으로하여 거래 내역을 구매자에게 투명하게 제공하므로써 정보의 비대칭성 문제를 개선한 중고거래 플랫폼을 개발하였다. 언제 어디서든 편리하게 접근할수 있게 모바일 환경인 android 어플리케이션을 개발하였다.
  ### 4. 사용법
  (블록체인서버)
  트러플 설치
npm install -g truffle

가나슈 설치
npm install -g ganache-cli

가나슈 서버 실행(IP로 실행) --db는 데이터 저장 경로
ganache-cli --host 222.222.222.222 -d --db ./blockchainData -i 9999 --deterministic --mnemonic="myth like bonus scare over problem client lizard pioneer submit female collect"
로컬호스트로 실행 --db는 데이터 저장 경로
ganache-cli -d --db ./blockchainData -i 9999 --deterministic --mnemonic="myth like bonus scare over problem client lizard pioneer submit female collect"
--deterministic 과 --mnemonic 옵션은 재실행해도 지갑 주소가 그대로 나오게 하기 위함

서버 실행후 스마트 컨트랙트 배포
(truffle-config.js 파일의 host : 에 127.0.0.1(로컬호스트) 또는 IP 입력
truffle compile
truffle migrate

(안드로이드)

1.	터미널에서 다음 명령을 입력합니다. git clone https://github.com/dpcks0509/OnionMarket
2.	Android Studio에서 import Project를 통해 clone한 폴더를 가져옵니다.
3.	Emulator의 가상기기를 이용하거나 실제 device를 연결하여 실행합니다.
(주의사항 : 블록체인 및 서버가 작동중이어야 정상적으로 작동합니다.)



## TEAM
  #### - 박예찬   
   studentNum: 202055643  
   git: https://github.com/dpcks0509
   e-mail: dpcks0509@gmail.com 
     
  #### - 손지훈  
   studentNum: 202055644  
   git: https://github.com/jihoon50  
   e-mail: thswlgns50@naver.com  
     
  #### - 채수철  
   studentNum: 202055652
   git: https://github.com/cotncjf2
   e-mail: cotncjf2@naver.com 



## POSTER
![figure](/images/capstone_poster.jpg)
