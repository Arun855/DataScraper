package com.amoghmc.redditDataScraper;

import net.dean.jraw.models.*;
import net.dean.jraw.pagination.DefaultPaginator;
import net.dean.jraw.pagination.Paginator;
import net.dean.jraw.pagination.SearchPaginator;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;

public class RedditResult {
	private final MainJFormFrame mainJFormFrame;
	private final MyRedditClient myRedditClient;
	private final AllFilters filterArrayList;
	private String subredditTextField;
	private String nsfw;
	private String spoiler;
	private final DateTimeFormatter dateFormatter;
	private int index;

	public RedditResult(MainJFormFrame mainJFormFrame, MyRedditClient myRedditClient) {
		this.mainJFormFrame = mainJFormFrame;
		this.myRedditClient = myRedditClient;
		filterArrayList = new AllFilters();
		dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss");
		index = 1;

		nsfw = "No";
		spoiler = "No";
	}

	public void addFilters() {
		subredditTextField = mainJFormFrame.getSubredditTextField().getText().replaceAll(" ", "");
		String keywordTextField = mainJFormFrame.getKeywordTextField().getText();
		if (mainJFormFrame.getNoNsfwCheckBox().isSelected()) {
			filterArrayList.addFilter(new NoNsfwFilter());
		}
		if (mainJFormFrame.getNoSpoilerCheckBox().isSelected()) {
			filterArrayList.addFilter(new NoSpoilerFilter());
		}
		if (mainJFormFrame.getScoreFilterCheckBox().isSelected()) {
			int scoreMin = Integer.parseInt(mainJFormFrame.getScoreMinTextField().getText().replaceAll(",", ""));
			int scoreMax = Integer.parseInt(mainJFormFrame.getScoreMaxTextField().getText().replaceAll(",", ""));
			if (scoreMax == 0) {
				filterArrayList.addFilter(new ScoreFilter(scoreMin));
			} else {
				filterArrayList.addFilter(new ScoreFilter(scoreMin, scoreMax));
			}
		}
		if (mainJFormFrame.getCommentCountFilterCheckBox().isSelected()) {
			int commentMin = Integer.parseInt(mainJFormFrame.getCommentCountMinTextField().getText().replaceAll(",", ""));
			int commentMax = Integer.parseInt(mainJFormFrame.getCommentCountMaxTextField().getText().replaceAll(",", ""));
			if (commentMax == 0) {
				filterArrayList.addFilter(new CommentCountFilter(commentMin));
			} else {
				filterArrayList.addFilter(new CommentCountFilter(commentMin, commentMax));
			}
		}

		if (subredditTextField.equals("")) {
			//filterArrayList.addFilter(new SubredditFilter(subredditTextField.split(",")));
			subredditTextField = "all";
		}
		if (mainJFormFrame.getRegexCheckBox().isSelected()) {
			filterArrayList.addFilter(new KeywordFilter(keywordTextField));
		}
		else if (mainJFormFrame.getKeywordCheckBox().isSelected()) {
			filterArrayList.addFilter(new KeywordFilter(keywordTextField.replaceAll(" ", "").split(",")));
		}
		filterArrayList.addFilter(new DuplicateFilter());
	}

	public Comparator<Submission> getSortSettings() {
		Comparator<Submission> comparator;
		if (mainJFormFrame.getAzSort().isSelected()) {
			comparator = new CompareTitle();
		} else if (mainJFormFrame.getZaSort().isSelected()) {
			comparator = new CompareTitle().reversed();
		} else if (mainJFormFrame.getScoreSortMin().isSelected()) {
			comparator = new CompareScore();
		} else {
			mainJFormFrame.getScoreSortMax().setSelected(true);
			comparator = new CompareScore().reversed();
		}
		return comparator;
	}

	public void display() {
		Paginator<Submission> paginator;
		if (mainJFormFrame.getKeywordCheckBox().isSelected()) {
			paginator = buildSearchPaginator();
		}
		else if (mainJFormFrame.getRegexCheckBox().isSelected()) {
			paginator = buildRegexPaginator();
		}
		else {
			paginator = buildDefaultPaginator();
		}

		int pages = Integer.parseInt(mainJFormFrame.getPageTextField().getText().replaceAll(",", ""));
		for (int k = 0; k < pages; k++) {
			Listing<Submission> nextPage = paginator.next();
			Comparator<Submission> comparator = getSortSettings();
			nextPage.sort(comparator);

			for (Submission s : nextPage) {
				if (filterArrayList.satisfies(s)) {
					if (s.isNsfw()) {
						nsfw = "Yes";
					}
					if (s.isSpoiler()) {
						spoiler = "Yes";
					}
					mainJFormFrame.getResultTextArea().append("Index: "
							+ index
							+ "\nSearch at: "
							+ dateFormatter.format(LocalDateTime.now())
							+ "\nTitle: "
							+ s.getTitle().replace('’', '\'').replace('—', '-')
							+ "\nScore: "
							+ s.getScore()
							+ "\nComment Count: "
							+ s.getCommentCount()
							+ "\nSubreddit: "
							+ s.getSubreddit()
							+ "\nURL: "
							+ s.getUrl()
							+ "\nPermalink: "
							+ "https://www.reddit.com" + s.getPermalink()
							+ "\nNSFW: "
							+ nsfw
							+ "\nSpoiler: "
							+ spoiler
							+ "\n"
							+ "\n");
					index++;
				}
			}
		}
		filterArrayList.clear();
	}

	private SearchPaginator buildSearchPaginator() {
		return myRedditClient
				.getMyclient()
				.subreddit(subredditTextField)
				.search()
				.query(mainJFormFrame.getKeywordTextField().getText())
				.sorting((SearchSort) Objects.requireNonNull(mainJFormFrame.getRedditSortComboBox().getSelectedItem()))
				.timePeriod((TimePeriod) Objects.requireNonNull(mainJFormFrame.getTimeComboBox().getSelectedItem()))
				.syntax(SearchPaginator.QuerySyntax.CLOUDSEARCH)
				.build();
	}

	public DefaultPaginator<Submission> buildDefaultPaginator() {
		return myRedditClient
				.getMyclient()
				.subreddit(subredditTextField)
				.posts()
				.sorting((SubredditSort) Objects.requireNonNull(mainJFormFrame.getRedditSortComboBox().getSelectedItem()))
				.timePeriod((TimePeriod) Objects.requireNonNull(mainJFormFrame.getTimeComboBox().getSelectedItem()))
				.build();
	}

	public DefaultPaginator<Submission> buildRegexPaginator() {
		return myRedditClient
				.getMyclient()
				.subreddit(subredditTextField)
				.posts()
				.sorting(SubredditSort.TOP)
				.timePeriod(TimePeriod.ALL)
				.build();
	}

	public void setIndex(int newIndex) {
		index = newIndex;
	}

}
