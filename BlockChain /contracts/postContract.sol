// SPDX-License-Identifier: MIT
pragma solidity ^0.8.2;

contract PostContract {
    struct Post {
        string userId;
        string userNickname;
        string userPhone;
        string postId;
        string postImageUrl;
        string postTitle;
        string postPrice;
        string postContent;
        uint256 createdAt;
    }

    mapping(uint256 => Post) public posts;
    uint256 public postCount;

    function createPost(
        string memory _userId,
        string memory _userNickname,
        string memory _userPhone,
        string memory _postId,
        string memory _postImageUrl,
        string memory _postTitle,
        string memory _postPrice,
        string memory _postContent,
        uint256 _createdAt
    ) public {
        postCount++;
        posts[postCount] = Post({
            userId: _userId,
            userNickname: _userNickname,
            userPhone: _userPhone,
            postId: _postId,
            postImageUrl: _postImageUrl,
            postTitle: _postTitle,
            postPrice: _postPrice,
            postContent: _postContent,
            createdAt: _createdAt
        });
    }

    function getPost(uint256 _postId) public view returns (string memory, string memory, uint256,string memory, string memory) {
        require(_postId > 0 && _postId <= postCount, "Invalid post ID");
        Post memory post = posts[_postId];
        return (post.userId, post.postId, post.createdAt, post.postPrice, post.postImageUrl);
    }

    function getPostsByName(string memory _userId) public view returns (Post[] memory, uint256) {
        uint256 count = 0;
        
        for (uint256 i = 1; i <= postCount; i++) {
            if (keccak256(bytes(posts[i].userId)) == keccak256(bytes(_userId))) {
                count++;
            }
        }

        Post[] memory filteredPosts = new Post[](count);
        
        uint256 currentIndex = 0;

        for (uint256 i = 1; i <= postCount; i++) {
            if (keccak256(bytes(posts[i].userId)) == keccak256(bytes(_userId))) {
                Post memory post = posts[i];
                filteredPosts[currentIndex] = post;
                currentIndex++;
            }
        }

        return (filteredPosts, count);
    }
}

