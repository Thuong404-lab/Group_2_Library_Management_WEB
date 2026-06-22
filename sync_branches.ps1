git add src/main/java/com/lms/entity/*.java
git commit -m "fix: Add missing no-argument constructors for JPA entities"
git push origin main

$branches = git branch -r | Where-Object { $_ -match "origin/" -and $_ -notmatch "HEAD" -and $_ -notmatch "main" } | ForEach-Object { $_.Trim().Replace("origin/", "") }

foreach ($branch in $branches) {
    Write-Host "============================="
    Write-Host "Syncing branch: $branch"
    git checkout $branch
    git pull origin $branch
    
    # Try merging main
    $mergeResult = git merge main -m "chore: Sync main to $branch (JPA constructors fix)" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Merge conflict or error in $branch. Aborting merge."
        git merge --abort
    } else {
        git push origin $branch
    }
}

git checkout main
