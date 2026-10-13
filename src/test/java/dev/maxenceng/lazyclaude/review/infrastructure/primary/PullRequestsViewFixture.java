package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.bareId;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.headBranch;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.longerThanTheBranch;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.sharingTheIdPrefix;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.siblingHeadBranch;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestTitle;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.secondPullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.secondPullRequestTitle;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.shortPullRequestTitle;
import static dev.maxenceng.lazyclaude.review.published.ReasonFixture.reason;

import java.util.List;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestView.PullRequestViewBuilder;
import dev.maxenceng.lazyclaude.review.published.CheckName;
import dev.maxenceng.lazyclaude.review.published.CheckNameFixture;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;

public final class PullRequestsViewFixture {

    private PullRequestsViewFixture() {}

    public static CheckView checkView(CheckName name, CheckOutcome outcome) {
        return CheckView.builder().name(name).outcome(outcome).build();
    }

    public static PullRequestViewBuilder pullRequestViewBuilder() {
        return PullRequestView.builder()
                .number(pullRequestNumber())
                .title(pullRequestTitle())
                .head(headBranch())
                .draft(false)
                .checks(List.of());
    }

    public static PullRequestView pullRequestView() {
        return pullRequestViewBuilder().build();
    }

    public static PullRequestView draftPullRequestView() {
        return pullRequestViewBuilder().draft(true).build();
    }

    public static PullRequestView shortTitledDraftPullRequestView() {
        return pullRequestViewBuilder().title(shortPullRequestTitle()).draft(true).build();
    }

    public static PullRequestView passedPullRequestView() {
        return pullRequestViewBuilder()
                .checks(List.of(checkView(CheckNameFixture.lint(), CheckOutcome.PASSED)))
                .build();
    }

    public static PullRequestView failedPullRequestView() {
        return pullRequestViewBuilder()
                .checks(List.of(
                        checkView(CheckNameFixture.build(), CheckOutcome.FAILED),
                        checkView(CheckNameFixture.contract(), CheckOutcome.PENDING),
                        checkView(CheckNameFixture.lint(), CheckOutcome.PASSED)))
                .build();
    }

    public static PullRequestView pendingPullRequestView() {
        return pullRequestViewBuilder()
                .checks(List.of(
                        checkView(CheckNameFixture.contract(), CheckOutcome.PENDING),
                        checkView(CheckNameFixture.lint(), CheckOutcome.PASSED)))
                .build();
    }

    public static PullRequestView draftFailedPullRequestView() {
        return pullRequestViewBuilder()
                .draft(true)
                .checks(List.of(checkView(CheckNameFixture.build(), CheckOutcome.FAILED)))
                .build();
    }

    public static PullRequestView siblingPullRequestView() {
        return pullRequestViewBuilder()
                .number(secondPullRequestNumber())
                .title(secondPullRequestTitle())
                .head(siblingHeadBranch())
                .checks(List.of(checkView(CheckNameFixture.lint(), CheckOutcome.PASSED)))
                .build();
    }

    public static PullRequestView sharingTheIdPrefixPullRequestView() {
        return pullRequestViewBuilder().head(sharingTheIdPrefix()).checks(List.of(checkView(CheckNameFixture.lint(), CheckOutcome.PASSED))).build();
    }

    public static PullRequestView bareIdPullRequestView() {
        return pullRequestViewBuilder().head(bareId()).checks(List.of(checkView(CheckNameFixture.lint(), CheckOutcome.PASSED))).build();
    }

    public static PullRequestView longerHeadPullRequestView() {
        return pullRequestViewBuilder().head(longerThanTheBranch()).checks(List.of(checkView(CheckNameFixture.lint(), CheckOutcome.PASSED))).build();
    }

    public static OpenPullRequestsView openPullRequestsViewOf(PullRequestView... pullRequests) {
        return new OpenPullRequestsView(List.of(pullRequests));
    }

    public static CheckingPullRequestsView checkingPullRequestsView() {
        return new CheckingPullRequestsView();
    }

    public static UnavailablePullRequestsView unavailablePullRequestsView() {
        return new UnavailablePullRequestsView(reason());
    }
}
