const PostContract = artifacts.require("PostContract");

contract("PostContract", (accounts) => {
  it("should create a new post", async () => {
    const postContractInstance = await PostContract.deployed();
    await postContractInstance.createPost("Post Title", "Post Content", 100, "https://example.com/image.jpg");
    const post = await postContractInstance.getPost(1);
    assert.equal(post[0], "Post Title", "Post name doesn't match");
    // 추가적인 검증을 수행하세요.
  });
});