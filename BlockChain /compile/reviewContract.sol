// SPDX-License-Identifier: MIT
pragma solidity ^0.8.2;

contract ReviewContract {
    struct Review {
        string userId;
        string reviewId;
        string reviewStar;
        string reviewText;
        string writerNickname;
        uint256 createdAt;
    }

    mapping(uint256 => Review) public reviews;
    uint256 public reviewCount;

    function createReview(
        string memory _userId,
        string memory _reviewId,
        string memory _reviewStar,
        string memory _reviewText,
        string memory _writerNickname,
        uint256 _createdAt
    ) public {
        reviewCount++;
        reviews[reviewCount] = Review({
            userId: _userId,
            reviewId: _reviewId,
            reviewStar: _reviewStar,
            reviewText: _reviewText,
            writerNickname: _writerNickname,
            createdAt: _createdAt
        });
    }

    function getReview(uint256 _postId) public view returns (string memory, string memory, uint256,string memory, string memory) {
        require(_postId > 0 && _postId <= reviewCount, "Invalid post ID");
        Review memory post = reviews[_postId];
        return (post.userId, post.reviewId, post.createdAt, post.reviewStar, post.reviewText);
    }

    function getReviewsByName(string memory _userId) public view returns (Review[] memory, uint256) {
        uint256 count = 0;
        
        for (uint256 i = 1; i <= reviewCount; i++) {
            if (keccak256(bytes(reviews[i].userId)) == keccak256(bytes(_userId))) {
                count++;
            }
        }

        Review[] memory filteredReviews = new Review[](count);
        
        uint256 currentIndex = 0;

        for (uint256 i = 1; i <= reviewCount; i++) {
            if (keccak256(bytes(reviews[i].userId)) == keccak256(bytes(_userId))) {
                Review memory review = reviews[i];
                filteredReviews[currentIndex] = review;
                currentIndex++;
            }
        }

        return (filteredReviews, count);
    }
}

