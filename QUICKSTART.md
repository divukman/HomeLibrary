# Quick Start Guide

Get up and running with Home Library Manager in 5 minutes!

## Prerequisites Check

Before starting, ensure you have:

1. **Java 17 or higher** installed
   ```bash
   java -version
   # Should show version 17 or higher
   ```

2. **Maven 3.6+** installed
   ```bash
   mvn -version
   # Should show Maven version
   ```

## Quick Setup (3 Steps)

### Step 1: Build the Application

```bash
mvn clean package
```

This downloads dependencies and builds the application. First run takes a few minutes.

### Step 2: Run the Application

**Linux/Mac:**
```bash
./run.sh
```

**Windows:**
```cmd
run.bat
```

**Or use Maven directly:**
```bash
mvn javafx:run
```

### Step 3: Add Your First Book

1. Click **"Add Book"** button
2. Enter at minimum:
   - **Title**: e.g., "The Pragmatic Programmer"
   - **Authors**: e.g., "David Thomas, Andrew Hunt"
3. Click **"Save"**

That's it! Your library is ready to use.

## Common First Steps

### Organize Your Books

1. **Add Categories**: When adding books, type category names like "Programming", "Fiction", "Reference"
2. **Set Shelf Locations**: Track where books are physically located (e.g., "Office A2")
3. **Add Tags**: Use tags for cross-categorization (e.g., "favorite", "to-read", "reference")

### Track Your Reading

- Check **"I have read this book"** for books you've finished
- Set **Rating** (0-5 stars) for books you've read
- Add **Notes** for personal thoughts or summaries

### Upload Cover Images

1. In the book form, click **"Upload Image"**
2. Select a JPG or PNG file
3. Image is automatically saved to the `covers/` directory

### Search Your Library

- Type in the search box to find books by title, author, or ISBN
- Use the filter dropdown to show only read/unread books
- Click column headers to sort

## File Locations

After first run, you'll have:

```
HomeLibrary/
├── homelibrary.db          # Your book database
├── covers/                 # Cover images
│   ├── 1.jpg
│   └── 2.jpg
├── logs/                   # Application logs
│   └── homelibrary.log
└── config.properties       # Configuration
```

## Optional: Amazon API Setup

To enable ISBN lookup and cover image fetching from Amazon:

1. Copy `config.properties.example` to `config.properties`
2. Sign up for [Amazon Product Advertising API](https://affiliate-program.amazon.com/assoc_credentials/home)
3. Add your credentials to `config.properties`:
   ```properties
   amazon.api.access.key=YOUR_KEY
   amazon.api.secret.key=YOUR_SECRET
   amazon.api.associate.tag=YOUR_TAG
   ```
4. Restart the application
5. Click **"Search Amazon"** when adding books

**Note**: Amazon API integration is optional and requires approval from Amazon.

## Keyboard Shortcuts

- **Double-click** a book to edit it
- **Enter** in search box to search
- Use **File → Refresh** to reload book list

## Troubleshooting

### "JavaFX runtime components are missing"

Always run with Maven:
```bash
mvn javafx:run
```

### Database is locked

Close all application instances and restart.

### Cover images not showing

Check that `covers/` directory exists and has proper permissions.

### Build fails

Ensure you have Java 17+ and Maven 3.6+ installed:
```bash
java -version
mvn -version
```

## Next Steps

- Read the full [README.md](README.md) for detailed documentation
- Review [home_library_spec.md](home_library_spec.md) for complete specifications
- Check `logs/homelibrary.log` for detailed application logs

## Data Safety

Your data is stored in `homelibrary.db`. To backup:

```bash
# Create a backup
cp homelibrary.db homelibrary.db.backup

# Or backup with timestamp
cp homelibrary.db "homelibrary.db.$(date +%Y%m%d)"
```

To restore, simply copy the backup file back.

## Import Sample Data (Optional)

Want to test with sample data? You can manually add a few books through the UI, or write a simple import script using the DAO classes.

## Getting Help

1. Check logs: `logs/homelibrary.log`
2. Review documentation: `README.md`
3. Examine code comments in source files

## Enjoy Your Library!

You're all set. Start organizing your book collection!
