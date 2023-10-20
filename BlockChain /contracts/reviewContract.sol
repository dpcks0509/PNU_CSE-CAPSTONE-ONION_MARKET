// SPDX-License-Identifier: MIT
pragma solidity ^0.8.2;

contract ReviewContract {
    struct Post {
        string userId;
        string reviewId;
        string reviewStar;
        string reviewText;
        string writerNickname;
        uint256 createdAt;
    }

    mapping(uint256 => Post) public posts;
    uint256 public postCount;

    function createReview(
        string memory _userId,
        string memory _reviewId,
        string memory _reviewStar,
        string memory _reviewText,
        string memory _writerNickname,
        uint256 _createdAt
    ) public {
        postCount++;
        posts[postCount] = Post({
            userId: _userId,
            reviewId: _reviewId,
            reviewStar: _reviewStar,
            reviewText: _reviewText,
            writerNickname: _writerNickname,
            createdAt: _createdAt
        });
    }

    function getReview(uint256 _postId) public view returns (string memory, string memory, uint256,string memory, string memory) {
        require(_postId > 0 && _postId <= postCount, "Invalid post ID");
        Post memory post = posts[_postId];
        return (post.userId, post.reviewId, post.createdAt, post.reviewStar, post.reviewText);
    }

    function getReviewsByName(string memory _userId) public view returns (Post[] memory, uint256) {
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

