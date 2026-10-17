package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.shell.MainViewScrollFixture.scrollOver;
import static dev.maxenceng.lazyclaude.shell.MainViewScrollFixture.scrollOverTwentyRowsInTen;
import static dev.maxenceng.lazyclaude.shell.MainViewScrollFixture.scrolledDown;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MainViewScrollTest {

    @Test
    void shouldStartAtTheTop() {
        assertThat(scrollOverTwentyRowsInTen().offset()).isZero();
    }

    @Test
    void shouldScrollOneRowDown() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();

        scroll.rowDown();

        assertThat(scroll.offset()).isEqualTo(1);
    }

    @Test
    void shouldStayAtTheTopWhenScrollingUpThere() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();

        scroll.rowUp();

        assertThat(scroll.offset()).as("no wrap to the bottom").isZero();
    }

    @Test
    void shouldStayAtTheBottomWhenScrollingDownThere() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.bottom();

        scroll.rowDown();

        assertThat(scroll.offset()).as("content height minus view height").isEqualTo(10);
    }

    @Test
    void shouldNotScrollContentShorterThanTheView() {
        MainViewScroll scroll = scrollOver(5, 10);

        scroll.rowDown();
        scroll.halfPageDown();
        scroll.bottom();

        assertThat(scroll.offset()).as("the maximum offset floors at zero").isZero();
    }

    @Test
    void shouldNotScrollContentExactlyAsTallAsTheView() {
        MainViewScroll scroll = scrollOver(10, 10);

        scroll.rowDown();

        assertThat(scroll.offset()).isZero();
    }

    @Test
    void shouldScrollHalfTheViewHeightDown() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();

        scroll.halfPageDown();

        assertThat(scroll.offset()).isEqualTo(5);
    }

    @Test
    void shouldScrollHalfTheViewHeightUp() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.bottom();

        scroll.halfPageUp();

        assertThat(scroll.offset()).isEqualTo(5);
    }

    @Test
    void shouldClampAHalfPageDownNearTheBottom() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scrolledDown(scroll, 8);

        scroll.halfPageDown();

        assertThat(scroll.offset()).as("stops at the maximum, not at 13").isEqualTo(10);
    }

    @Test
    void shouldClampAHalfPageUpNearTheTop() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scrolledDown(scroll, 3);

        scroll.halfPageUp();

        assertThat(scroll.offset()).isZero();
    }

    @Test
    void shouldScrollAtLeastOneRowForAHalfPageOfAOneRowView() {
        MainViewScroll scroll = scrollOver(5, 1);

        scroll.halfPageDown();

        assertThat(scroll.offset()).isEqualTo(1);
    }

    @Test
    void shouldGoToTheBottomThenBackToTheTop() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();

        scroll.bottom();
        assertThat(scroll.offset()).isEqualTo(10);
        scroll.top();

        assertThat(scroll.offset()).isZero();
    }

    @Test
    void shouldResetToTheTop() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.bottom();

        scroll.reset();

        assertThat(scroll.offset()).isZero();
    }

    @Test
    void shouldPullTheOffsetBackWhenTheContentShrinks() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.bottom();

        scroll.observe(12, 10);

        assertThat(scroll.offset()).as("a resize or a shorter ticket never leaves the offset past the end").isEqualTo(2);
    }

    @Test
    void shouldNotScrollBeforeAnythingHasBeenRendered() {
        MainViewScroll scroll = new MainViewScroll();

        scroll.rowDown();

        assertThat(scroll.offset()).isZero();
    }

    @Test
    void shouldFollowGrowingContentWhileAtTheBottom() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.bottom();

        scroll.follow(25, 10);

        assertThat(scroll.offset()).isEqualTo(15);
    }

    @Test
    void shouldHoldItsPlaceWhenScrolledUpWhileTheContentGrows() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.bottom();
        scroll.rowUp();

        scroll.follow(25, 10);

        assertThat(scroll.offset()).isEqualTo(9);
    }

    @Test
    void shouldFollowAgainOnceScrolledBackDownToTheBottom() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.bottom();
        scroll.rowUp();
        scroll.rowDown();

        scroll.follow(25, 10);

        assertThat(scroll.offset()).isEqualTo(15);
    }

    @Test
    void shouldFollowAgainOnTheBottomKey() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.bottom();
        scroll.halfPageUp();
        scroll.bottom();

        scroll.follow(25, 10);

        assertThat(scroll.offset()).isEqualTo(15);
    }

    @Test
    void shouldStopFollowingAtTheTop() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.bottom();
        scroll.top();

        scroll.follow(25, 10);

        assertThat(scroll.offset()).isZero();
    }

    @Test
    void shouldFollowAgainWhenAHalfPageDownReachesTheBottom() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.bottom();
        scroll.halfPageUp();
        scroll.halfPageDown();

        scroll.follow(25, 10);

        assertThat(scroll.offset()).isEqualTo(15);
    }

    @Test
    void shouldStartAFreshViewAtItsLatestRow() {
        MainViewScroll scroll = new MainViewScroll();

        scroll.follow(20, 10);

        assertThat(scroll.offset()).isEqualTo(10);
    }

    @Test
    void shouldFollowFromTheBottomAfterAReset() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.top();

        scroll.reset();
        scroll.follow(20, 10);

        assertThat(scroll.offset()).as("another session is shown from its latest entry").isEqualTo(10);
    }

    @Test
    void shouldNotFollowContentThatIsOnlyObserved() {
        MainViewScroll scroll = scrollOverTwentyRowsInTen();
        scroll.top();

        scroll.observe(30, 10);

        assertThat(scroll.offset()).as("a ticket that grows keeps its first rows in view").isZero();
    }
}
