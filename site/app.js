const fallbackStars = 1;
const starCount = document.querySelector("#star-count");

const formatStars = (count) =>
  new Intl.NumberFormat("en", { notation: count >= 1000 ? "compact" : "standard" }).format(count);

fetch("https://api.github.com/repos/Tygb99/phonepad", {
  headers: { Accept: "application/vnd.github+json" },
})
  .then((response) => {
    if (!response.ok) {
      throw new Error("GitHub repository metadata request failed");
    }
    return response.json();
  })
  .then((repo) => {
    if (typeof repo.stargazers_count === "number" && starCount) {
      starCount.textContent = formatStars(repo.stargazers_count);
    }
  })
  .catch(() => {
    if (starCount) {
      starCount.textContent = formatStars(fallbackStars);
    }
  });
