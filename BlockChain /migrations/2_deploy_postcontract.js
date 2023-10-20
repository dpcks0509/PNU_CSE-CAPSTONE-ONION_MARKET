const postcontract = artifacts.require("PostContract");

module.exports = function(deployer){
  deployer.deploy(postcontract);
};