const reviewcontract = artifacts.require("ReviewContract");

module.exports = function(deployer){
  deployer.deploy(reviewcontract);
};