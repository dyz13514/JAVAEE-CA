(() => {
    const form = document.getElementById('courseApplicationForm');
    if (!form) return;
    const selection = window.courseSelection || { courses: [] };
    const courses = (selection.courses || []).filter(course => !selection.locked
        || String(course.id) === String(selection.courseId));
    const title = document.getElementById('courseTitle');
    const category = document.getElementById('categoryId');
    const provider = document.getElementById('providerId');
    const courseId = document.getElementById('courseId');
    const fee = document.getElementById('fee');
    const halfDay = document.getElementById('halfDay');
    const sameTitle = (left, right) => left.toLowerCase() === right.toLowerCase();

    function fill(select, placeholder, options, selected, autoSelect) {
        select.replaceChildren(new Option(placeholder, ''));
        options.forEach(option => select.add(new Option(option.label, String(option.value))));
        select.value = selected == null ? '' : String(selected);
        if (!select.value && autoSelect && options.length === 1) select.value = String(options[0].value);
        select.disabled = options.length === 0;
    }

    function matchingTitle() {
        return courses.filter(course => sameTitle(course.title, title.value));
    }

    function updateFee() {
        title.setCustomValidity('');
        const course = matchingTitle().find(course => String(course.categoryId) === category.value
            && String(course.providerId) === provider.value);
        courseId.value = course ? course.id : '';
        fee.value = course ? Number(course.fee).toFixed(2) : '';
        const chosenCategory = matchingTitle().find(course => String(course.categoryId) === category.value);
        halfDay.disabled = !chosenCategory || !chosenCategory.halfDayAllowed;
        if (halfDay.disabled) halfDay.checked = false;
    }

    function updateProviders(selected) {
        const matches = matchingTitle().filter(course => String(course.categoryId) === category.value);
        const options = [...new Map(matches.map(course => [course.providerId,
            { value: course.providerId, label: course.providerName }])).values()];
        fill(provider, '-- Select Provider --', options, selected, true);
        updateFee();
    }

    function updateCategories(selected, selectedProvider) {
        const options = [...new Map(matchingTitle().map(course => [course.categoryId,
            { value: course.categoryId, label: course.categoryName + (course.halfDayAllowed ? ' (half-day allowed)' : '') }])).values()];
        fill(category, '-- Select Category --', options, selected, true);
        updateProviders(selectedProvider);
    }

    const titles = [...new Map(courses.map(course => [course.title.toLowerCase(), course.title])).values()];
    const selectedCourse = courses.find(course => String(course.id) === String(selection.courseId));
    const selectedTitle = selectedCourse ? selectedCourse.title : selection.title;
    const canonicalTitle = selectedTitle && titles.find(value => sameTitle(value, selectedTitle));
    fill(title, '-- Select Course --', titles.map(value => ({ value, label: value })), canonicalTitle, false);
    updateCategories(selectedCourse ? selectedCourse.categoryId : selection.categoryId,
        selectedCourse ? selectedCourse.providerId : selection.providerId);
    if (selection.locked) {
        title.disabled = true;
        category.disabled = true;
        provider.disabled = true;
    }

    title.addEventListener('change', () => updateCategories(null, null));
    category.addEventListener('change', () => updateProviders(null));
    provider.addEventListener('change', updateFee);
    form.addEventListener('submit', event => {
        if (!courseId.value) {
            event.preventDefault();
            title.setCustomValidity('Please select an available course, category and training provider.');
            title.reportValidity();
        }
    });
    title.addEventListener('change', () => title.setCustomValidity(''));

    if (courses.length === 0) {
        const message = document.createElement('p');
        message.setAttribute('role', 'alert');
        message.textContent = 'No courses are available. Please contact the administrator.';
        form.before(message);
        form.querySelector('button[type="submit"]').disabled = true;
    }
})();
